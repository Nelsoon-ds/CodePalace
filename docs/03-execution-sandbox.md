# 03 — Execution Sandbox

The single riskiest component: CodePalace runs untrusted user code by design. This doc is the contract for how that is done safely on a single VM.

## Threat model

| Threat | Vector | Mitigation |
|---|---|---|
| Sandbox escape → host compromise | Java/JS runtime 0-days, kernel paths | Ephemeral containers, read-only rootfs, non-root user in image, socket-proxy restricts Docker API verbs; audits: image pinning by digest |
| Network exfiltration / attack relay | outbound sockets from run code | `--network none` always; network access is *never* configurable |
| Resource exhaustion / DoS | infinite loops, fork bombs, giant allocations | CPU/mem/pids caps + wall-clock kill, per-user submission rate limits at `api` |
| Persistence between runs | malware surviving in the box | Fresh container per run; volume-less tmpfs; runner teardown on exit *and* timeout |
| Secret theft | env vars, docker socket, DB creds | Nothing user-relevant inside the run container. Only `code + tests`; egress none; socket proxy exposes no `exec`, no volume write |
| Hidden-suite theft | user imports/tests visibility | Suites are streamed into the container at run time only; users see results, never code ([privacy invariant](05-data-model-api.md#hidden-suite-privacy-invariant)) |
| Runner compromise (e.g. malicious payload via path traversal job id) | API ↔ runner messages | Internal-only network range in compose, message schema validated, no shell-out with interpolated strings |

**Residual risk is accepted and documented:** containers are not Firecracker/gVisor-grade. Accepted because: single-VM demo scope, no PII beyond email/hashes on the box, inbound attack surface is only nginx + `/api`.

## Runner images

| Image | Base | Contents | Owner |
|---|---|---|---|
| `codepalace/runner-node:22` | `node:22-alpine` | `vitest` pinned, non-root `kata` user, `/sandbox` workdir, entrypoint runs `vitest run --reporter=json` | built by CI from `sandbox-images/node/` |
| `codepalace/runner-java:25` (M4) | `eclipse-temurin:25-jre-alpine` | JUnit 5 console launcher prewarmed; dependencies vendored at image build | built from `sandbox-images/java/` |

Images are built once and pinned by digest on the VM — user runs never `docker build`.

Resource budgets (initial): Node gets `256MB / 1.0 CPU / 10s wall clock`; Java target is `512MB / 30s`, revisited with measurement data at M4.

## Run lifecycle (state machine)

```
QUEUED → PROVISIONING → RUNNING → COMPLETED
                     ↘ FAILED (tests failed)          ↘ TIMEOUT (wall clock) → torn down
                     ↘ INTERNAL_ERROR (runner/bus)     ↘ OOM_CONTAIN (Docker event)
```

`exec-runner` duties per run:

1. Acquire slot from local semaphore (**max 3 concurrent containers** — VM budget, see [06-devops.md](06-devops.md#vm-sizing)).
2. Create container: `--network none --read-only --memory 256m --cpus 1.0 --pids-limit 128 --workdir /sandbox --tmpfs /tmp:size=64m --tmpfs /sandbox/tmp:size=32m`, code + suite mounted read-only, stdout/stderr captured (tail-capped 64KB).
3. `docker start`, follow events; enforce wall clock with `docker kill` fallback.
4. Parse vitest JSON; emit run events; persist results row.
5. `docker rm -f` in all paths (finally block), release slot.

## Result streaming & persistence

- `exec-runner` → `api`: internal `POST /internal/run-events` (compose-internal network only).
- `api` → browser: **SSE** on `/api/runs/{id}/events`: `queued`, `provisioning`, `running`, `assertion` (incremental), `summary` (final: passed/failed counts, durations, first diff), `error`.
- Final results JSON stored on `runs.results_jsonb` (assertion list, timings, suite hash actually used).
- Console output is a *tail* only — full logs are not a feature.

```mermaid
stateDiagram-v2
    [*] --> QUEUED
    QUEUED --> PROVISIONING: runner acquires slot
    PROVISIONING --> RUNNING: container started
    RUNNING --> COMPLETED: vitest exit 0
    RUNNING --> FAILED: tests failed
    RUNNING --> TIMEOUT: wall clock exceeded
    RUNNING --> OOM_KILLED: memory cap
    RUNNING --> INTERNAL_ERROR: infrastructure fault
    COMPLETED --> [*]
    FAILED --> [*]
```

## Sandbox content layout (per run, tmpfs scratch)

```
/sandbox
├── src/            # user code (submitted)
├── tests/          # hidden suite (mounted at run time, deleted on teardown)
└── tmp/
```

Player code is imported by the suite; the suite is deleted post-run.

## JVM notes (M4)

- **Cold start is ~1–2s JVM boot** — acceptable inside a 30s cap for M4.
- **Warm pool is a v2 lever**, not M4 scope: keep N prestarted containers paused; runner punch-through with code injection. Only if measurements demand it.
- Alternative noted but rejected for v1: GraalVM native-image runners (complexity + reflection config for JUnit).

## Scaling notes (future)

Current single-runner, single-VM: queue on Postgres, semaphore local. Horizontal path: multiple `exec-runner` instances on different VMs, same Postgres `LISTEN/NOTIFY`, per-VM slot cap. Documented so M1 bus design does not prevent it.
