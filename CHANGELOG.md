# Changelog

## Unreleased

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
- Known gap: the container writable layer has no disk quota yet (`StorageOpt` not set). Memory, CPU and PID limits and the wall-clock watchdog land in Step 3.

### Phase 1, Step 1: Runner image (`sandbox-images/node/`)
- Base image pinned by digest: `node:22-alpine@sha256:fe4f6d74…9914`.
- vitest pinned to 5.0.5 (`--save-exact`), installed locally in `/sandbox`.
- Added `vitest.config.mjs`. Vitest 5's JSON reporter writes to a file by default, so the config sets `stdout: true`. It also moves the vite and vitest caches to `/tmp`.
- Entrypoint: `/sandbox/node_modules/.bin/vitest run --configLoader=runner`. `--configLoader=runner` stops vite writing a temp file next to the config.
