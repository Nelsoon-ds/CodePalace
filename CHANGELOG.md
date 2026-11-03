# Changelog

## Unreleased

### Docs & tooling: generation spike write-up
- Added `docs/09-kata-generation-spike-and-platform-design.md`: results of the Gemini kata-generation spike (branch `spike/kata-generation`, 5 of 10 katas passing all five checks in the latest run, 8 of 10 best), what the validation checks caught, the hidden-suite readability gap, and the design decisions for exercises (shared dataset plus at most two tasks) and the shared corpus.
- Added `opencode.json` registering the Context7 MCP server so documentation lookups are available in OpenCode sessions.

### Phase 1, Step 3: Watchdog & resource limits
- Resource caps in `createContainer`: 256MB memory with swap disabled (`MemorySwap` = `Memory`, both in bytes), 1.0 CPU, 128 PIDs.
- 10s wall-clock watchdog (`timeoutMs` option) kills the container; `runInSandbox` returns `timedOut`, `oomKilled` and `truncated` flags, since exit 137 alone is ambiguous.
- Output is streamed and tail-capped at 64KB per stream (`TailBuffer`) instead of loading all logs after exit. Known limitation: vitest's JSON report is on stdout, so a report over 64KB would be cut off and fail to parse.
- Added `runner/limits-check.js` (negative tests): infinite loop, PID exhaustion, unbounded fork bomb, memory hog (OOM), log flood, disk fill. All pass and no containers are left behind.
- Disk quota gap closed. `StorageOpt` is accepted but not enforced by this Docker's overlayfs/containerd driver (a 300MB write succeeded with `size=100m`), so instead every path `kata` can write to is a size-capped tmpfs: added `/sandbox/.vitest`, `/var/tmp` and `/home/kata` (16MB each) alongside `/tmp` (64MB) and `/sandbox/tmp` (32MB). `limits-check.js` now verifies per-path write caps; `/sandbox`, `/etc`, `/usr/local` and `/` accept 0MB.
- The fork bomb took about 1s longer than the timeout to be killed under load.

### Phase 1, Step 2: Standalone container runner
- Added `runner/test-sandbox.js`: creates a container via dockerode, streams `src/solution.mjs` and `tests/suite.test.mjs` in as an in-memory tar (`tar-stream`), starts it, demuxes stdout/stderr, parses the vitest JSON and force-removes the container in `finally`.
- **Decision: writable root filesystem instead of `ReadonlyRootfs`.** Docker's archive API (`PUT /containers/{id}/archive`) rejects uploads to containers with a read-only rootfs, even into tmpfs paths. Compensating hardening:
  - `CapDrop: ["ALL"]` and `no-new-privileges`
  - `NetworkDisabled: true`
  - Non-root user `kata` (UID 10001)
  - Uploaded files, `node_modules` and the vitest config are root-owned and read-only (0444), so user code can't tamper with the suite or vitest
  - Only `/tmp`, `/sandbox/tmp` and `/sandbox/.vitest` are writable by `kata`; the `/tmp` tmpfs mounts are `noexec,nosuid` with size caps
- Added `runner/security-check.js`: runs a probe suite inside the real sandbox config and asserts non-root, no capabilities, `no_new_privs`, no network/DNS, no writes to suite/src/vitest/`/etc`/`/usr`, `noexec` on `/tmp`, no Docker socket, no secrets in env (18/18 pass). `runInSandbox` is now exported from `test-sandbox.js`.
- Docs `03` and `08` updated to reflect the writable-root decision and the image changes.
- Revisit options (volume upload, or stdin tar pipe) when hardening in Step 3 / Phase 2. Volume upload needs the socket proxy to allow volumes.
- Memory, CPU and PID limits and the wall-clock watchdog landed in Step 3, along with the disk cap (see above).

### Phase 1, Step 1: Runner image (`sandbox-images/node/`)
- Base image pinned by digest: `node:22-alpine@sha256:fe4f6d74…9914`.
- vitest pinned to 5.0.5 (`--save-exact`), installed locally in `/sandbox`.
- Added `vitest.config.mjs`. Vitest 5's JSON reporter writes to a file by default, so the config sets `stdout: true`. It also moves the vite and vitest caches to `/tmp`.
- Entrypoint: `/sandbox/node_modules/.bin/vitest run --configLoader=runner`. `--configLoader=runner` stops vite writing a temp file next to the config.
