import pg from "pg";
import { runInSandbox } from "./test-sandbox.js";
const { Client, Pool } = pg;

const client = new Client({
  user: "codepalace",
  password: "password",
  host: "localhost",
  port: 5432,
  database: "codepalace",
});

const pool = new Pool({
  user: "codepalace",
  password: "password",
  host: "localhost",
  port: 5432,
  database: "codepalace",
});

// FUNCTIONS
async function setupListener() {
  client.on("notification", (msg) => {
    if (msg.channel === "jobs_channel") {
      console.log("New job added:", msg.payload);
      drainQueue().catch(console.error);
    }
  });

  await client.query("LISTEN jobs_channel");
  console.log("Listening for new jobs...");

  await requeueStaleJobs();
  await drainQueue();

  await client.query("SELECT pg_notify('jobs_channel', 'New job added!')");
}

// A worker that dies mid-run leaves its job in CLAIMED forever. Put those back on the queue
// unless they already failed too often.
async function requeueStaleJobs() {
  const { rowCount } = await pool.query(
    `UPDATE jobs
        SET status = 'QUEUED', claimed_at = NULL
      WHERE status = 'CLAIMED'
        AND attempts < 3
        AND claimed_at < now() - interval '10 minutes'`,
  );
  if (rowCount > 0) {
    console.log(`Requeued ${rowCount} stale job(s)`);
  }
}

async function loadRunFiles(runId) {
  const { rows } = await pool.query(
    `SELECT
       submission.code AS solution_code,
       suite.code AS test_code
     FROM runs r
     JOIN submissions submission ON submission.id = r.submission_id
     JOIN test_suites suite ON suite.id = r.suite_id
     WHERE r.id = $1`,
    [runId],
  );

  if (rows.length === 0) {
    throw new Error(`Run not found: ${runId}`);
  }

  const run = rows[0];

  return [
    {
      name: "src/solution.mjs",
      content: run.solution_code,
    },
    {
      name: "tests/suite.test.mjs",
      content: run.test_code,
    },
  ];
}

async function claimNextJob() {
  const poolClient = await pool.connect();
  try {
    await poolClient.query("BEGIN");

    const { rows } = await poolClient.query(
      "SELECT * FROM jobs WHERE status = 'QUEUED' ORDER BY created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
    );

    if (rows.length === 0) {
      console.log("No jobs to claim.");
      await poolClient.query("COMMIT");
      return;
    }

    const job = rows[0];
    console.log("Claiming job:", job);

    const result = await poolClient.query(
      "UPDATE jobs SET status = 'CLAIMED', claimed_at = NOW(), \
    attempts = attempts + 1 WHERE id = $1 \
    RETURNING *",
      [job.id],
    );

    await poolClient.query("COMMIT");
    return result.rows[0];
  } catch (error) {
    console.error("Error claiming job:", error);
    await poolClient.query("ROLLBACK");
    throw error;
  } finally {
    poolClient.release();
  }
}

async function drainQueue() {
  try {
    let job;

    while ((job = await claimNextJob())) {
      console.log("Claimed job:", job.id);
      const files = await loadRunFiles(job.run_id);

      const result = await runInSandbox(files);
      let runStatus;

      if (result.timedOut) {
        runStatus = "TIMED_OUT";
      } else if (result.oomKilled) {
        runStatus = "OOM_KILLED";
      } else if (result.results === null) {
        runStatus = "INTERNAL_ERROR";
      } else if (result.exitCode === 0) {
        runStatus = "COMPLETED";
      } else {
        runStatus = "FAILED";
      }

      // result.results is null when the sandbox produced no report at all; optional chaining keeps
      // the worker from crashing on that path (it used to leave the job stuck in CLAIMED).
      const durationMs =
        (result.results?.testResults?.[0]?.endTime -
          result.results?.testResults?.[0]?.startTime) || null;

      console.log("Duration (ms):", durationMs);
      console.log("Job completed:", job.id);
      console.log("Job result:", result);
      console.log("Exit code:", result.exitCode);
      console.log("Result:", result.results);

      // Keep the tail only: the container output can be huge, and the backend sanitizes it again.
      const outputTail = `${result.stdout || ""}\n${result.stderr || ""}`.slice(-8000);

      try {
        await pool.query("UPDATE jobs SET status = 'COMPLETED' WHERE id = $1", [
          job.id,
        ]);
        await pool.query(
          "UPDATE runs SET status = $1, exit_code = $2, duration_ms = $3, results_jsonb = $4, output_tail = $5 WHERE id = $6",
          [
            runStatus,
            result.exitCode,
            durationMs ? durationMs : 0,
            result.results === null ? null : JSON.stringify(result.results),
            outputTail,
            job.run_id,
          ],
        );
      } catch (error) {
        console.error("Error updating job/run status:", error);
      }
    }
  } catch (error) {
    console.error("Error draining queue:", error);
  }
}

// Setup the database connection and start listening for notifications
await client.connect();
setupListener().catch(console.error);

//await client.end()
