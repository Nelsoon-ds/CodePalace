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
  if (normalized.includes("TIMEOUT") || normalized.includes("TIMED_OUT")) return "Execution timed out before the time limit.";
  if (normalized.includes("OOM")) return "Execution stopped because it exceeded the memory limit.";
  if (normalized.includes("INTERNAL_ERROR")) return "The runner encountered an internal error.";
  if (normalized.includes("FAILED")) return "Run failed.";
  if (normalized.includes("COMPLETED")) return "Run completed.";
  if (normalized.includes("RUNNING")) return "Code is running…";
  if (normalized.includes("QUEUED")) return "Run queued…";
  return status || "Waiting for run events…";
}

export function ResultsConsole({ runId, run, events, streamState, error }: Props) {
  // 1. Because parseData spreads the JSON payload, the event itself IS the RunResult
  const latestStatusData = events.reduce<Partial<RunResult> | undefined>((latest, event) => {
    if (event.event === "status") {
      // Cast the event directly to RunResult since the properties are flattened
      return event as unknown as Partial<RunResult>;
    }
    return latest;
  }, undefined);

  // 2. Use the live stream data if available, otherwise fall back to the initial fetch
  const activeData = latestStatusData ?? run ?? {};

  // 3. Extract the assertions array
  const assertions = activeData.assertions ?? [];

  // 4. Resolve the current status string (reading directly from event.status)
  const status = events.reduce<string | undefined>((latest, event) => {
    return event.status ?? latest;
  }, activeData.status);

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
                {typeof activeData.durationMs === "number" && <span className={styles.runDuration}>{activeData.durationMs} ms</span>}
              </div>
              {error && <p className={styles.inlineError}>{error}</p>}

              {assertions.length > 0 ? (
                  <div className={styles.assertions}>
                    {assertions.map((assertion, index) => (
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
            </>
        ) : (
            <p className={styles.consoleHint}>
              Run your solution to see assertion results. Hidden test-suite code is never shown here.
            </p>
        )}
      </section>
  );
}
