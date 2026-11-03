# Autoresearch: speed up runner/test-sandbox.js

## Goal
Reduce the wall-clock time of `node runner/test-sandbox.js` (the full
`runInSandbox` round trip) without weakening sandbox isolation or changing
what the script reports.

## Scope
Mutable (you may edit):
- `runner/test-sandbox.js`

Read-only (never edit):
- Everything in `autoresearch/`, including `bench.sh` and `results.tsv`
- `runner/security-check.js` and `runner/limits-check.js`
- The `files` fixture (the solution and test content) in `test-sandbox.js`
- All sandbox limits in `HostConfig`: `NetworkDisabled`, `CapDrop`,
  `SecurityOpt`, `PidsLimit`, `Memory`, `MemorySwap`, `NanoCpus`, `Tmpfs`
  (including the `noexec`/`nosuid` flags)
- `TIMEOUT_MS`, `MEMORY_BYTES`, `IMAGE`, `OUTPUT_TAIL_BYTES`
- `runner/package.json` and `node_modules`. Do not install packages or change
  Docker configuration.

## Metric
Run `bash autoresearch/bench.sh` from the repo root. It prints:

    METRIC median_ms=<number>
    GATE=PASS|FAIL

Lower `median_ms` is better. A run only counts if `GATE=PASS`.

## Gate (all must hold)
- Five runs of `runner/test-sandbox.js` each exit 0 and print
  `passed: 2, failed: 0`.
- `runner/security-check.js` reports `0 failed`. It probes network, caps,
  filesystem, noexec, and the docker socket.
- Before each commit, `git diff` shows no removed or weakened line in
  `HostConfig` or the limit constants.

## Setup (already done, do not repeat)
- Branch `autoresearch/sandbox-speed` exists.
- Baseline: run `bash autoresearch/bench.sh` once and record it as the first
  row of `autoresearch/results.tsv`, with status `baseline`.

## Experiment loop
Repeat up to 25 experiments:
1. Read `autoresearch/results.tsv` and `git log` to see what has been tried.
2. Make one focused change to `runner/test-sandbox.js`.
3. Commit it with a short description.
4. Run `bash autoresearch/bench.sh > autoresearch/run.log 2>&1`. Do not print
   the full log. Read only the `METRIC` and `GATE` lines.
5. Append a row to `autoresearch/results.tsv`.
6. Keep the commit only if `GATE=PASS` and `median_ms` is at least 3% lower
   than the best so far. Otherwise run `git reset --hard HEAD~1`.
   The 3% threshold guards against Docker timing noise.
7. If the script crashes or the gate fails, log it as `crash` or `fail` and reset.

Stop when you hit 25 experiments or 10 discards in a row. Then print a
summary: the baseline, the best commit and its median, and what worked
and what didn't.

## Ideas (starting points, not a checklist)
- Look for sequential `await`s whose inputs don't depend on each other.
- Look for work done before it is needed.
- Check whether the log-streaming setup adds latency at the start or end.
- Do not shrink the fixture, the test content, or the output limits to win time.

## Rules
- Never edit tests or fixtures to make them faster.
- Never loosen a sandbox limit to gain speed.
- Keep a change only when the improvement is clearly above noise.
- Do not ask whether to continue during the loop. Stop only at the stop condition.
- Never run `git reset` on anything other than the autoresearch branch's own
  experiment commits.
