# 08 — Build Path & Reading Curriculum

This document outlines the recommended step-by-step implementation path for CodePalace. The order follows a **risk-first, outside-in strategy**: de-risking the core execution engine and message queue before building out domain APIs, the React UI, or integrating AI providers.

Each phase breaks down into concrete activities accompanied by curated documentation, architectural articles, and reference guides.

---

## Phase 1: The Sandbox Engine (Spike & De-Risk)

> **Objective:** Prove that untrusted code execution, ephemeral Docker lifecycle control, in-memory code injection, and headless Vitest parsing work standalone in a Node.js script before writing any backend or frontend code.

### Step 1: Build the Runner Docker Image (`sandbox-images/node/`)
* **Activities:**
  * Create a Dockerfile based on `node:22-alpine`.
  * Pre-install `vitest` pinned to a specific version (5.0.5, local install in `/sandbox`); pin the base image by digest.
  * Create a non-privileged system user (`kata`, UID 10001) and set it as `USER kata`.
  * Configure `/sandbox` as the default working directory, root-owned; only `tmp/` and `.vitest/` are writable by `kata`.
  * Add `vitest.config.mjs` (JSON reporter with `stdout: true`, caches moved to `/tmp`; vitest 5 otherwise writes JSON to a file).
  * Define entrypoint: `vitest run --configLoader=runner` (the config supplies the JSON reporter).
* **Curated Readings & Docs:**
  * [Docker Official: Best practices for Dockerfile instructions](https://docs.docker.com/build/building/best-practices/) — Deep dive on non-root users, layer caching, and minimizing attack surface.
  * [OWASP Docker Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html) — Key security rules: non-root user execution, read-only root filesystems, and dropping capabilities.
  * [Vitest CLI & Reporters Documentation](https://vitest.dev/guide/reporters.html) — Configuration and schema format of the Vitest JSON reporter (`--reporter=json`).

### Step 2: Standalone Container Runner (`runner/test-sandbox.js`)
* **Activities:**
  * Write a standalone Node script utilizing Docker's Engine API (via Docker socket or HTTP client).
  * Create a container in the stopped state with `--network none`, `--cap-drop ALL`, `no-new-privileges`, and tmpfs scratch spaces. **Not `--read-only`**: the archive API rejects uploads to read-only containers (see [03 — writable root decision](03-execution-sandbox.md#decision-writable-root-filesystem-no---read-only)).
  * Stream in-memory `src/solution.mjs` and `tests/suite.test.mjs` (ESM, root-owned, mode 0444) into `/sandbox` using the Docker Archive API (`PUT /containers/{id}/archive`).
  * Verify isolation with `runner/security-check.js`.
  * Start container, capture stdout/stderr, parse the output JSON, and forcefully remove the container in a `finally` block.
* **Curated Readings & Docs:**
  * [Docker Engine API Reference: Containers](https://docs.docker.com/reference/api/engine/version/v1.45/#tag/Container) — Specifically `POST /containers/create`, `PUT /containers/{id}/archive`, `POST /containers/{id}/start`, and `GET /containers/{id}/logs`.
  * [npm tar-stream documentation](https://www.npmjs.com/package/tar-stream) — Creating in-memory `.tar` archives from raw JavaScript strings to stream into Docker.
  * [Dockerode GitHub & Documentation](https://github.com/apocas/dockerode) — The standard Node.js client library for Docker Engine API (if not using raw HTTP).

### Step 3: Hard Watchdog & Resource Limits
* **Activities:**
  * Configure Docker runtime caps: `--memory 256m`, `--cpus 1.0`, `--pids-limit 128`.
  * Implement an active Node.js `setTimeout` (10s wall-clock kill switch).
  * Write negative test cases: test an infinite loop (`while(true){}`) and a fork bomb to verify the watchdog kills the container cleanly.
  * Buffer stdout/stderr with a 64KB tail cap to prevent memory bloat on noisy scripts.
* **Curated Readings & Docs:**
  * [Docker Resource Constraints Guide](https://docs.docker.com/engine/containers/resource_constraints/) — Understanding cgroups, memory limits, swap restrictions, and OOM killer events (exit code 137).
  * [Node.js Stream Documentation](https://nodejs.org/api/stream.html) — Implementing bounded memory buffers and transform streams to tail-cap output.

---

## Phase 2: Database & Brokerless Queue

> **Objective:** Connect the runner to Postgres so jobs can be queued, processed via `LISTEN/NOTIFY`, and stored without requiring Redis or RabbitMQ.

### Step 1: Docker Compose Foundation (`compose.yaml`)
* **Activities:**
  * Configure Postgres 16 with a persistent data volume.
  * Configure `tecnativa/docker-socket-proxy` on an internal Docker bridge.
  * Restrict socket proxy permissions to container create/start/delete/inspect only (`CONTAINERS=1`, `POST=1`, `VOLUMES=0`, `EXEC=0`).
* **Curated Readings & Docs:**
  * [Tecnativa docker-socket-proxy GitHub](https://github.com/Tecnativa/docker-socket-proxy) — Read the security model and verb allowlist configuration.
  * [Docker Compose Networking Reference](https://docs.docker.com/compose/how-tos/networking/) — Configuring internal bridge networks (`internal: true`) to isolate background services.

### Step 2: Database Schema & Migrations
* **Activities:**
  * Create migrations for: `workspaces`, `sessions`, `katas`, `test_suites`, `submissions`, `runs`, and `jobs`.
  * Configure the `runs.results_jsonb` and `test_suites.validation_stats` JSONB columns.
  * Add required indexes (foreign keys, `jobs.status`).
* **Curated Readings & Docs:**
  * [PostgreSQL Docs: JSON Types & Indexing](https://www.postgresql.org/docs/current/datatype-json.html) — `json` vs `jsonb`, GIN index strategies, and JSON path operators.
  * [Flyway by Redgate: Database Migration Concepts](https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html) — Versioned SQL migration best practices.

### Step 3: Runner Daemon (`exec-runner`)
* **Activities:**
  * Set up a persistent Postgres connection via `pg` in Node.
  * Issue `LISTEN jobs_channel`.
  * Add a startup drain query: `SELECT * FROM jobs WHERE status = 'QUEUED' ORDER BY created_at ASC` to recover jobs missed during restarts.
  * Add a concurrency semaphore restricting active jobs to `maxConcurrency = 3`.
  * On job receipt: fetch user code & test suite, execute inside sandbox container, write `results_jsonb` back to `runs`, and mark job as `COMPLETED`.
* **Curated Readings & Docs:**
  * [PostgreSQL Docs: LISTEN](https://www.postgresql.org/docs/current/sql-listen.html) and [NOTIFY](https://www.postgresql.org/docs/current/sql-notify.html) — Transactional notification guarantees and channel semantics.
  * [node-postgres (`pg`) Notification Handling](https://node-postgres.com/apis/client#events) — Managing persistent connection events and payload deserialization.
  * [p-limit (npm)](https://github.com/sindresorhus/p-limit) — Lightweight Promise-based concurrency limiting in JavaScript.

### Step 4: End-to-End Bus Validation
* **Activities:**
  * Manually insert a mock submission and job row via `psql`.
  * Fire `NOTIFY jobs_channel`.
  * Verify in terminal logs that `exec-runner` picks up the job, executes the sandbox, updates `runs`, and cleans up.

---

## Phase 3: Spring Boot Core API (M1 Backend)

> **Objective:** Expose secure REST endpoints for workspace management, code submission, and real-time Server-Sent Events (SSE) streaming.

### Step 1: Project Bootstrap (`api/`)
* **Activities:**
  * Scaffold Spring Boot 3+ (Java 21 or 25) with Maven.
  * Dependencies: Spring Web, Spring Data JPA, Spring Security, Flyway, PostgreSQL Driver.
  * Configure connection pool (HikariCP) and JPA properties.
* **Curated Readings & Docs:**
  * [Spring Boot 3 Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) — Architecture and dependency configuration.
  * [Vlad Mihalcea: Mapping JSON Objects in Hibernate 6](https://vladmihalcea.com/spring-boot-3-hibernate-6-json/) — Using `@JdbcTypeCode(SqlTypes.JSON)` to bind Java records to Postgres `jsonb`.

### Step 2: Auth & Workspaces CRUD
* **Activities:**
  * Implement user registration and login with Argon2 password hashing.
  * Generate and validate stateless JWTs (24h lifespan).
  * Create CRUD endpoints for `/api/workspaces` and `/api/sessions`.
  * Enforce workspace ownership invariants on all session operations.
* **Curated Readings & Docs:**
  * [Spring Security Architecture Guide](https://docs.spring.io/spring-security/reference/servlet/architecture.html) — Security filter chain, authentication providers, and bearer token filters.
  * [Baeldung: Password Hashing with Argon2 in Java](https://www.baeldung.com/java-password-hashing-argon2) — Why Argon2 beats BCrypt for modern password storage.
  * [Auth0: JWT Best Current Practices (RFC 8725)](https://auth0.com/blog/a-look-at-the-latest-draft-for-jwt-bcp-and-rfc-8725/) — Stateless tokens and claim validation.

### Step 3: Seed JS Katas & Hidden-Suite Isolation
* **Activities:**
  * Seed 2 JavaScript katas (e.g. *Reverse a String*, *Check Palindrome*) with starter code and hidden Vitest suites.
  * Ensure the API DTO/mapping layer strictly omits test suite code from any client-facing response ([Hidden-Suite Privacy Invariant](05-data-model-api.md#hidden-suite-privacy-invariant)).
* **Curated Readings & Docs:**
  * [Martin Fowler: Data Transfer Object (DTO) Pattern](https://martinfowler.com/eaaCatalog/dataTransferObject.html) — Preventing database entity leakage across API boundaries.
  * [MapStruct Reference Documentation](https://mapstruct.org/documentation/stable/reference/html/) — Compile-time type-safe object mapping for Java.

### Step 4: Submission Enqueueing & SSE Event Hub
* **Activities:**
  * Implement `POST /api/sessions/{id}/submissions`.
  * In a single database transaction: store submission, create `run (status=QUEUED)`, insert `job`, and trigger `NOTIFY jobs_channel, '{"run_id":"..."}'`.
  * **Payload Rule:** Keep `NOTIFY` payload minimal (<8KB Postgres limit) — send only the `run_id`, never raw source code.
  * Create internal endpoint `POST /internal/run-events` (called by `exec-runner` to report status transitions).
  * Create client SSE endpoint `GET /api/runs/{id}/events` using `SseEmitter` to stream live assertions.
* **Curated Readings & Docs:**
  * [Spring Framework: Asynchronous Request Processing with SseEmitter](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-sse) — Managing emitter lifecycles, heartbeats, and client disconnects.
  * [Baeldung: Server-Sent Events with Spring](https://www.baeldung.com/spring-server-sent-events) — Hands-on patterns for publishing events to `SseEmitter` instances across threads.

---

## Phase 4: React Workspace Shell (M1 Frontend)

> **Objective:** Deliver a responsive, IDE-like browser workspace featuring Monaco Editor, debounced autosave, and a live assertion console.

### Step 1: Scaffold Frontend (`frontend/`)
* **Activities:**
  * Initialize React + TypeScript application with Vite and Tailwind CSS.
  * Install `@tanstack/react-query`, `zustand`, `lucide-react`, and `@monaco-editor/react`.
  * Set up Axios or Fetch API client with automatic JWT header injection.
* **Curated Readings & Docs:**
  * [Vite Getting Started Guide](https://vite.dev/guide/) — Fast HMR and build optimizations.
  * [TanStack Query Quick Start (React Query)](https://tanstack.com/query/latest/docs/framework/react/overview) — Replacing manual `useState`/`useEffect` data fetching with caching and mutations.
  * [TkDodo's Practical React Query Blog](https://tkdodo.eu/blog/practical-react-query) — Must-read guide on structuring React Query hooks and keys.

### Step 2: Auth & Workspace Selection Views
* **Activities:**
  * Build simple Login / Register forms.
  * Build a workspace dashboard displaying the user's workspaces and active sessions.
  * Store auth tokens in memory with silent renewal on 401.

### Step 3: Workspace Shell & Monaco Editor (Single-File UX)
* **Activities:**
  * Build focused three-pane layout: session sidebar (left), single-file Monaco editor (center), results panel (bottom/right).
  * Adopt single-file editor model (LeetCode/Codewars style): no multi-tab or file-tree complexity in M1. Switching katas mounts the selected code directly into the single editor instance.
  * Integrate `@monaco-editor/react`, binding language to `javascript`.
  * Implement debounced autosave: send `PUT /api/sessions/{id}/draft` after 800ms idle or on window blur.
  * Use Zustand to store active session and console layout state.
* **Curated Readings & Docs:**
  * [@monaco-editor/react Documentation](https://github.com/suren-atoyan/monaco-react) — Multi-model support, editor configuration, and event listeners.
  * [Zustand Getting Started](https://zustand.docs.pmnd.rs/getting-started/introduction) — Minimal boilerplate global UI state management.

### Step 4: Submission & Live Results Console
* **Activities:**
  * Add "Run Tests" action button triggering `POST /api/sessions/{id}/submissions`.
  * Receive `202 Accepted { runId }` and connect to `EventSource('/api/runs/' + runId + '/events')`.
  * Render incoming assertions incrementally:
    * 🟡 `queued` / `running` status badges
    * 🟢 Passed assertions with execution times
    * 🔴 Failed assertions with expandable error diffs
* **Curated Readings & Docs:**
  * [MDN: Using Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events) — Native `EventSource` API, custom event listeners, and automatic reconnections.
  * [React Patterns: Handling Real-time SSE Streams](https://web.dev/articles/eventsource-basics) — Memory management and avoiding React re-render bottlenecks during high-frequency events.

---

## Phase 5: AI Generation & Validation Loop (M2)

> **Objective:** Transition from static katas to dynamic AI generation with a mandatory automated validation loop ensuring generated suites are functionally discriminating.

### Step 1: Provider-Agnostic Interface (`KataGenerator`)
* **Activities:**
  * Define `KataGenerator` interface in Spring Boot (`generate(request)`, `critique(request, failure)`).
  * Implement `OpenAiCompatibleKataGenerator` using strict JSON schema responses (`response_format: { type: "json_schema" }`).
  * Force output schema: `{ title, statement_md, starter_code, reference_solution, test_suite, tags[], category }`.
* **Curated Readings & Docs:**
  * [OpenAI Docs: Structured Outputs Guide](https://platform.openai.com/docs/guides/structured-outputs) — How constrained decoding guarantees schema adherence.
  * [Anthropic Docs: Tool Use (Function Calling)](https://docs.anthropic.com/en/docs/build-with-claude/tool-use) — Enforcing structured schema responses across LLM providers.
  * [Spring AI: Structured Output Converters](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html) — (Optional) Native Spring abstraction for model-agnostic outputs.

### Step 2: Automated Sandbox Validation Loop (Regex Mutations)
* **Activities:**
  * Run the AI-generated `reference_solution` against the `test_suite` inside the Phase 1 execution runner; verify it passes 100%.
  * Apply lightweight regex string mutations in Spring Boot (constant return `return null;`, inverted comparisons `===` → `!==`, off-by-one `<` → `<=`). No complex AST parsing needed.
  * Run the suite against each mutant; verify the suite fails on all mutants.
  * If validation fails: trigger `critique()` prompt to repair (max 2 retries), or mark kata as `DRAFT`.
* **Curated Readings & Docs:**
  * [Mutation Testing Concepts (Pitest Documentation)](https://pitest.org/) — Understanding why suites must fail on broken code to prove test quality.
  * [Google Research: Large Language Models for Test Case Generation and Repair](https://research.google/pubs/pub51631/) — Automated test generation critique loops and failure localization.

### Step 3: Generation UI Modal
* **Activities:**
  * Add a generation modal in the frontend: language, category, theme, and difficulty level.
  * Display generation progress and link the newly generated kata directly into the workspace.

---

## Phase 6: Search, Agent & Cloud Deployment (M3 -> M4)

> **Objective:** Add full-text and semantic retrieval, multi-turn AI tool calling, and production deployment on a single cloud VM.

### Step 1: Kata Discovery & Full-Text Search (M3)
* **Activities:**
  * Add Postgres `tsvector` column and GIN index on kata statements and descriptions.
  * Implement trigram matching (`pg_trgm`) for fuzzy keyword search.
  * Expose search endpoint `/api/katas?query=&tag=&category=` to browse the catalog.
* **Curated Readings & Docs:**
  * [PostgreSQL Docs: Full-Text Search](https://www.postgresql.org/docs/current/textsearch-intro.html) — Creating search vectors, document ranking (`ts_rank`), and dictionaries.
  * [PostgreSQL Docs: pg_trgm Extension](https://www.postgresql.org/docs/current/pgtrgm.html) — Trigram indexing for fuzzy string search.

### Step 2: Semantic Search & Multi-Turn Agent Loop (M4)
* **Activities:**
  * Enable `pgvector` extension in Postgres and create an HNSW index on statement embeddings.
  * Implement tool-calling agent loop in Spring Boot with tools: `search_test_suites`, `get_kata_details`, `save_kata_draft`.
  * Stream tool invocation traces back to the user via SSE for transparency.
* **Curated Readings & Docs:**
  * [pgvector GitHub: Getting Started with Vector Similarity](https://github.com/pgvector/pgvector) — Vector operations (`<=>` cosine distance) and HNSW index tuning.
  * [OpenAI Docs: Function Calling & Agent Loops](https://platform.openai.com/docs/guides/function-calling) — Multi-turn conversation handling with tool results.

### Step 3: Single-VM Cloud Deployment (GCP Compute Engine)
* **Activities:**
  * Provision a Google Cloud Compute Engine VM (Ubuntu, 4 vCPU, 8GB RAM).
  * Configure VPC firewall rules: allow TCP ports 80 (HTTP) and 443 (HTTPS).
  * Configure Nginx reverse proxy with automated Let's Encrypt TLS (Certbot).
  * Set up GitHub Actions CI/CD to build images, push to GHCR, and deploy via SSH.
* **Curated Readings & Docs:**
  * [Google Cloud: Compute Engine Linux VM Quickstart](https://cloud.google.com/compute/docs/quickstart-linux) — Creating instances, configuring firewalls, and managing SSH keys.
  * [Docker Compose in Production Best Practices](https://docs.docker.com/compose/how-tos/production/) — Environment file isolation, volume backup, and restart policies.
  * [DigitalOcean / EFF: Certbot with Nginx on Ubuntu](https://certbot.eff.org/instructions?ws=nginx&os=ubuntufocal) — Automated SSL/TLS certificate issuing and renewals.
