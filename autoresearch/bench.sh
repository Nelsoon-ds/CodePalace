#!/usr/bin/env bash
# Read-only for the agent. Run from the repo root.
# Times the sandbox round trip (5 runs, median) and runs the security probe as a gate.
set -uo pipefail

times=()
gate=PASS

for i in 1 2 3 4 5; do
  start=$(date +%s%3N)
  out=$(node runner/test-sandbox.js 2>&1)
  code=$?
  end=$(date +%s%3N)
  times+=($((end - start)))
  if [[ $code -ne 0 ]] || ! grep -q "passed: 2, failed: 0" <<<"$out"; then
    gate=FAIL
  fi
done

sec=$(cd runner && timeout 150 node security-check.js 2>&1)
if ! grep -q "0 failed" <<<"$sec"; then
  gate=FAIL
fi

median=$(printf '%s\n' "${times[@]}" | sort -n | sed -n '3p')
echo "METRIC median_ms=$median"
echo "GATE=$gate"
