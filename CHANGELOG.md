# Changelog

## Unreleased

### Backend hardening: auth, ownership and schema (branch `feat/backend-hardening`)
- **The API is now actually protected.** `SecurityConfig` permitted every `/api/**` route, so the JWT was decorative: anyone could list all workspaces, read any session (including the autosaved draft), submit code and read any run. Only `/api/auth/register`, `/api/auth/login` and `/api/auth/refresh` are public now; everything else needs a valid access token, and an unauthenticated request gets a JSON 401 instead of a 500.
- **Ownership is enforced on every user-owned resource.** `Workspace` now maps its owner (`@ManyToOne User`), and the services resolve resources through owner-scoped queries (`findOwnedSession`, `findOwnedRun`, `findByIdAndUser_Id`). Another user's session, workspace, run or draft answers 404 instead of handing over the data. `POST /api/workspaces` and `DELETE /api/workspaces/{id}` were empty stubs and are implemented.
- **Client-facing DTOs replace raw entities.** `GET /api/katas` no longer returns the `reference_solution` (the answer key) and only returns published + public katas; the `query`/`difficulty` filters are implemented, `tag`/`category` still have no columns to filter on. `GET /api/workspaces` returns the caller's workspaces with their sessions, and the session list now carries the kata title. The `test_suites.code` column and `User.passwordHash` are `@JsonIgnore`d as a safety net.
- **Vitest code frames are stripped from the client-visible diff.** Vitest appends the failing line of the hidden test file to every failure message, and `output_tail` carried the raw container stderr - both leaked hidden suite source into the browser (hidden-suite privacy invariant, docs/05). The diff is now sanitized and truncated, and the runner only stores the last 8KB of output.
- **Malformed or expired tokens return 401, not 500.** The JWT filter parsed the token outside a try/catch, so `Authorization: Bearer garbage` produced a 500 with a stack trace. The token is now parsed once, and access tokens carry a `typ` claim so a refresh token can never be replayed as an access token.
- **Token refresh.** `POST /api/auth/refresh` exchanges a refresh token (30 days, sent in the body or as a bearer token) for a new access token. The frontend already called this endpoint; it did not exist.
- **Input validation and a real error contract.** `UserCreation`/`UserLogin`/`CodeSubmission`/`DraftRequest` are validated (`@Email`, `@Size(min = 12)`), emails are normalised so `Test@x.com` and `test@x.com` cannot become two accounts, and a `@RestControllerAdvice` maps domain failures to 400/401/404/409 instead of 500. Duplicate registration answers 409.
- **Schema is owned by Flyway.** Added `flyway-core` (+ `spring-boot-flyway`) with `ddl-auto=validate`, so a column that exists in Java but not in the database fails at startup instead of at the first query. `V2__add_draft_code.sql` adds the `sessions.draft_code` column the entity always mapped but no migration created; `V3__align_runs_duration_and_constraints.sql` aligns `runs.duration_ms` with the fractional value the runner reports and adds the CHECK constraints the development database never received. The compose init-script mount is gone - Flyway applies the schema instead. The development database is baselined at V1, so no data is lost.
- **Fixed the seed script**: it inserted into `workspaces (user_id, user_name)` while the column is `workspace_name`, and it stored `'not-a-hash'` as the password. It now seeds usable Argon2 hashes for the password `codepalace-dev`.
- **Submission writes are transactional.** Submission, run and job are written in one transaction, and the `pg_notify` that wakes the runner is delivered by Postgres at commit, so the runner can never see the notification before the job row exists.
- **SSE no longer leaks a task per subscription.** The poller was scheduled with a zero initial delay, so a subscription to an already finished run left a task polling the database once per second for the lifetime of the JVM. The pool size and the stream timeout are configurable now.
- **Runner fixes**: the worker crashed when the sandbox produced no report (`result.results` was null), which left the job stuck in `CLAIMED` forever; stale `CLAIMED` jobs are requeued at startup (max 3 attempts) and the container output is tail-capped.
- **Tests**: 41 tests (`mvn test`), including a Testcontainers integration test that boots the whole application against a real PostgreSQL, applies the migrations and validates the Hibernate mapping, plus slice tests for the auth rules, the ownership rules and the hidden-suite sanitization.

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
