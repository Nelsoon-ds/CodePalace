import pg from "pg";
import { runInSandbox, files } from "./test-sandbox.js";
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
  
  await drainQueue()

  await client.query("SELECT pg_notify('jobs_channel', 'New job added!')");
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
  }
  finally {
    poolClient.release();
  }
}


async function drainQueue() {
  try {
    let job;
    while ((job = await claimNextJob(client))) {
      console.log("Claimed job:", job.id);
    }
} catch (error) {
    console.error("Error draining queue:", error);
  }};


// Setup the database connection and start listening for notifications
await client.connect();
setupListener().catch(console.error);

await client.query("INSERT into run")

async function testRun() {

}
//setupListener().catch(console.error);



//await client.end()

//console.log(await client.query('SELECT NOW()'))
//console.log(await client.query('SELECT * FROM katas'))
/* console.log(await client.query(
  `INSERT INTO katas (title, statement_md, difficulty, programming_language, starter_code, created_by)
   VALUES ($1, $2, $3, $4, $5, $6)
   RETURNING id, title`,
  ['Test Kata', 'This is a test kata.', 'easy', 'js', '', 'user']
)) */
