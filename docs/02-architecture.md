# 02 — Architecture

Services run on a single VM under docker compose. Names here are canonical across all docs.

## Service topology

```mermaid
flowchart LR
    U[Browser] -->|HTTPS| NG[nginx]
    NG -->|/| FE[frontend: static React bundle]
    NG -->|/api proxy| API[api: Spring Boot :8080]
    API --> DB[(db: Postgres :5432)]
    API -->|run job via LISTEN/NOTIFY| DB
    RUNNER[exec-runner: Node :8090] -->|LISTEN job queue| DB
    RUNNER -->|persist results| DB
    RUNNER -->|docker.sock via socket-proxy| SP[socket-proxy]
    SP --> DOCK[Docker daemon]
    DOCK --> C1[runner-node :22 ephemeral xN]
    DOCK --> C2[runner-java :25 ephemeral xN - phase 2]
    AI[External AI provider] -.->|HTTPS outbound| API
```

| Service | Runtime | Responsibility |
|---|---|---|
| `nginx` | nginx | TLS termination, static files, `/api` reverse proxy |
| `frontend` | nginx + static React (Vite) | Monaco editor, workspace shell, SSE consumption |
| `api` | Spring Boot (Java 25) | Auth, domain CRUD, job enqueue, AI orchestration, SSE hub |
| `exec-runner` | Node | Spawn/cap/torn-down ephemeral exec containers, stream run events |
| `db` | Postgres 16 | Domain data, job queue (`LISTEN/NOTIFY`), run results |
| `socket-proxy` | tecnativa/docker-socket-proxy | Expose only `POST /containers/create`, `POST /containers/start`, `DELETE /containers`, `GET /containers/json` to `exec-runner` |

**No Redis/RabbitMQ on purpose.** The run-job bus is a Postgres table + `LISTEN/NOTIFY`: one row per job, `exec-runner` keeps a `LISTEN jobs_channel` connection. One VM, one runner → a broker is accidental complexity (documented for future scaling in [03-execution-sandbox.md](03-execution-sandbox.md#scaling-notes-future)).

## Flow 1 — Run a submission (M1)

```mermaid
sequenceDiagram
    participant FE as frontend
    participant API as api
    participant DB as db
    participant RUN as exec-runner
    participant D as Docker
    FE->>API: POST /api/sessions/{id}/submissions {code}
    API->>API: JWT check, rate limit
    API->>DB: INSERT submission, run(status=QUEUED)
    API-->>FE: 202 {runId} + SSE subscribe
    API->>DB: INSERT job(run_id), NOTIFY jobs_channel
    RUN->>DB: receives job, loads run + hidden suite
    RUN->>D: run container (no-network, caps, tmpfs)
    D-->>RUN: stream status, exit code
    RUN->>DB: UPDATE run(status, results, duration)
    RUN->>API: POST internal /events (progress/completion)
    API-->>FE: SSE events (queued/running/assertions/summary)
```

## Flow 2 — Generate a kata, mode 1 (M2)

```mermaid
sequenceDiagram
    participant FE as frontend
    participant API as api
    participant AI as AI provider
    participant RUN as exec-runner
    participant DB as db
    FE->>API: POST /api/generation/kata {language, category, theme, difficulty}
    API->>DB: INSERT generation_job(status=RUNNING), quota check
    API->>AI: structured generation prompt
    AI-->>API: kata JSON (statement, starter, reference, tests, tags)
    API->>DB: stage draft, INSERT run job (suite vs reference)
    API->>RUN: validate suite in sandbox
    RUN-->>API: reference passes? mutations fail?
    alt validation fails (≤2 retries)
        API->>AI: critique + repair prompt
    else exhausted
        API->>DB: kata status=DRAFT (human review)
    end
    API-->>FE: kata published, workspace-attachable
```

## Flow 3 — Agent retrieval + construction, mode 2 (M4)

```mermaid
sequenceDiagram
    participant FE as frontend
    participant API as api (agent loop)
    participant DB as db / tool layer
    participant AI as AI provider
    FE->>API: POST /api/agent/messages {goal}
    API->>AI: agent turn 1 (tools available)
    AI-->>API: tool call search_test_suites {tags: [recursion, strings], language: JS}
    API->>DB: keyword/tag query (pgvector later)
    API->>AI: tool results (suite summaries + stats)
    AI-->>API: construct novel kata/suite informed by retrieved corpus
    API->>API: same validation loop as mode 1
    API-->>FE: SSE: tool traces + final kata
```

## Key invariants

1. **Hidden suites never cross the api boundary to the client.** Suites travel only: `db → exec-runner → container`. See [05-data-model-api.md](05-data-model-api.md#hidden-suite-privacy-invariant).
2. **All code execution happens in ephemeral containers.** `api` never evals/compiles user code in-process.
3. **AI is behind an interface**, so M4 mode 2 and provider swaps do not fork the codebase ([04-ai-and-retrieval.md](04-ai-and-retrieval.md)).
4. **Job queue survives restarts** (rows in Postgres, re-`NOTIFY`ed on runner boot with `status=QUEUED` scan).
