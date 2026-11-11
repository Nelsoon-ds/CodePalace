import type { Assertion, RunEvent, RunResult } from "../api/types";
import styles from "../styles/Workspace.module.css";

type Props = {
  runId?: string;
  run?: RunResult;
  events: RunEvent[];
  streamState: "idle" | "connecting" | "live" | "closed" | "error";
  error?: string;
};

function statusMessage(status = "") {
  const normalized = status.toUpperCase();
  if (normalized.includes("TIMEOUT") || normalized.includes("TIMED_OUT")) {
    return "Execution timed out before the time limit.";
  }
  if (normalized.includes("OOM")) {
    return "Execution stopped because it exceeded the memory limit.";
  }
  if (normalized.includes("INTERNAL_ERROR")) return "The runner encountered an internal error.";
  if (normalized.includes("FAILED")) return "Run failed.";
  if (normalized.includes("COMPLETED")) return "Run completed.";
  if (normalized.includes("RUNNING")) return "Code is running…";
  if (normalized.includes("QUEUED")) return "Run queued…";
  return status || "Waiting for run events…";
}

function eventAssertion(event: RunEvent): Assertion | null {
  if (event.event === "assertion" && event.data && typeof event.data === "object") {
    const data = event.data as Partial<Assertion>;
    if (typeof data.name === "string" && typeof data.passed === "boolean") {
      return {
        name: data.name,
        passed: data.passed,
        ms: data.ms,
        diff: data.diff,
      };
    }
  }
  if (typeof event.name === "string" && typeof event.passed === "boolean") {
    return { name: event.name, passed: event.passed, ms: event.ms, diff: event.diff };
  }
  return null;
}

export function ResultsConsole({ runId, run, events, streamState, error }: Props) {
  const assertions = [
    ...(run?.assertions ?? []),
    ...events.map(eventAssertion).filter((value): value is Assertion => value !== null),
  ];
  const status = events.reduce<string | undefined>((latest, event) => {
    if (event.event === "status" || event.status) {
      const dataStatus =
        event.data && typeof event.data === "object"
          ? (event.data as { status?: string }).status
          : undefined;
      return event.status ?? dataStatus ?? (typeof event.data === "string" ? event.data : latest);
    }
    return latest;
  }, run?.status);
  const uniqueAssertions = assertions.filter(
    (assertion, index) =>
      assertions.findIndex(
        (candidate) =>
          candidate.name === assertion.name &&
          candidate.passed === assertion.passed &&
          candidate.ms === assertion.ms,
      ) === index,
  );

  return (
    <section className={styles.results} aria-label="Run results">
      <header className={styles.consoleHeader}>
        <div>
          <span className={styles.eyebrow}>OUTPUT</span>
          <h2>{runId ? "Test results" : "Console"}</h2>
        </div>
        <span className={`${styles.liveDot} ${streamState === "live" ? styles.live : ""}`} />
      </header>
      {runId ? (
        <>
          <div className={styles.runStatus}>
            <span className={styles.statusIcon}>{status?.toUpperCase().includes("COMPLETED") ? "✓" : "◌"}</span>
            <span>{statusMessage(status)}</span>
            {typeof run?.durationMs === "number" && <span className={styles.runDuration}>{run.durationMs} ms</span>}
          </div>
          {error && <p className={styles.inlineError}>{error}</p>}
          {uniqueAssertions.length > 0 ? (
            <div className={styles.assertions}>
              {uniqueAssertions.map((assertion, index) => (
                <details
                  className={styles.assertion}
                  key={`${assertion.name}-${index}`}
                  open={!assertion.passed && Boolean(assertion.diff)}
                >
                  <summary>
                    <span className={assertion.passed ? styles.pass : styles.fail}>
                      {assertion.passed ? "✓" : "×"}
                    </span>
                    <span className={styles.assertionName}>{assertion.name}</span>
                    <span className={styles.assertionTime}>
                      {assertion.ms === undefined ? "—" : `${assertion.ms.toFixed(1)} ms`}
                    </span>
                  </summary>
                  {!assertion.passed && assertion.diff && (
                    <pre className={styles.diff}>{assertion.diff}</pre>
                  )}
                </details>
              ))}
            </div>
          ) : (
            <p className={styles.consoleHint}>
              {streamState === "error" ? "Could not connect to the run event stream." : "Assertions appear here as your code runs."}
            </p>
          )}
          {run?.outputTail && <pre className={styles.output}>{run.outputTail}</pre>}
        </>
      ) : (
        <p className={styles.consoleHint}>
          Run your solution to see assertion results. Hidden test-suite code is never shown here.
        </p>
      )}
    </section>
  );
}
