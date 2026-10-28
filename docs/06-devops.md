# 06 — DevOps & Stack Decisions

## Monorepo layout

```
CodePalace/
├── README.md
├── docs/                      # this doc set
├── api/                       # Spring Boot (Java 25, Maven)
├── runner/                    # exec-runner (Node 22)
├── frontend/                  # React + Vite
├── sandbox-images/
│   ├── node/Dockerfile        # runner-node:22
│   └── java/Dockerfile        # runner-java:25 (M4)
├── compose.yaml               # prod-ish, local parity
└── .github/workflows/ci.yaml
```

One clone = full stack runnable. Tags/releases are image digests per service.

## compose topology

```yaml
services:
  nginx:          # 80/443, TLS via certbot volume, proxies /api → api:8080
  frontend:       # static build, internal only
  api:            # 8080 internal; JWT secret, DB url, AI key via env_file
  exec-runner:    # 8090 internal; talks to db + socket-proxy only
  db:             # postgres:16, volume pgdata, LISTEN/NOTIFY
  socket-proxy:   # tecnativa/docker-socket-proxy; limited verb allowlist
                  #   (containers create/start/delete/inspect only, no volumes, no exec)
```

No run traffic touches the edge except via `nginx`; `exec-runner` has no published port. All containers on one internal bridge except `nginx`.

## Database decision matrix (locked: Postgres)

| Criterion | Postgres 16 | MySQL 8 |
|---|---|---|
| JSONB results runs | native, queryable | JSON type, ok but weaker operators |
| Full-text taxonomy search (M3) | tsvector + pg_trgm, simple | fulltext ngram, less flexible |
| Semantic search (M4) | **pgvector** (one less moving part than a sidecar idx) | needs external vector service |
| Job bus via LISTEN/NOTIFY (**no broker**) | yes | no practical equivalent |
| Spring Data JPA maturity | excellent | excellent |
| Familiarity from SmartBio | lower | higher (existing MySQL muscle memory) |

MySQL wins nothing structural; LISTEN/NOTIFY and pgvector are what the architecture leans on ([02](02-architecture.md#service-topology), [04](04-ai-and-retrieval.md#retrieval-design)). Verdict: Postgres despite lower personal familiarity. H2 (test profile) for unit tests, MySQL not involved.

## VM sizing

| Component | Budget |
|---|---|
| api | ~512MB |
| exec-runner | ~256MB |
| db | ~1GB |
| nginx + socket-proxy | ~128MB |
| exec containers (3 max concurrent JS) | 3 × 256MB = 768MB |
| headroom for Java phase (JVM runs 512MB × 3) | +1.5GB |

**Verdict:** start at **4 vCPU / 8GB VPS** (any major provider, ~€15-25/mo). Container concurrency cap 3 lives in the runner ([03](03-execution-sandbox.md#run-lifecycle-state-machine)); bump only after measuring real durations. During M2 AI bursts, generation is API-bound (no sandbox), so bursts coexist with runs.

## CI/CD (GitHub Actions, mirror of prior workflow habits)

1. `api`: mvn test (H2)
2. `runner`: node --test / vitest
3. `frontend`: vitest + build
4. Build images `api`, `runner`, `frontend`, `runner-node` → push GHCR (digest pins)
5. **Manual gate deploy** job: SSH VM → `docker compose pull && docker compose up -d`

Secrets in Actions: GHCR token, VM SSH key. Secrets on VM: `.env` (DB password, JWT secret, `AI_API_KEY`, `AI_PROVIDER`). All secrets compose-injected at runtime — nothing baked into images.

## Backups & observability

- Nightly `pg_dump` to local disk rotation (7 days) — suite corpus + accounts is the only valuable state.
- `api` actuator health `/actuator/health` consumed by compose healthchecks; Uptime-Kuma optional.
- Runner logs via docker logging driver (json-file, rotated, 10MB rolling stop).

## Local Development Setup (Developer PC / WSL 2)

All initial development (M0 through M3) is designed to run **100% locally on the developer machine** without incurring cloud costs:

1. **Backing Services (Docker Compose):**
   * Run only infrastructure dependencies via `compose.yaml`:
     ```bash
     docker compose up -d db socket-proxy
     ```
2. **Local Runtimes (Fast Feedback & Debugging):**
   * **`api`:** Runs natively via `./mvnw spring-boot:run` (Java 21/25, connects to local Postgres on `localhost:5432`).
   * **`exec-runner`:** Runs natively via `node runner.js` (Node 22+, connects to local Postgres and `localhost:2375` socket proxy).
   * **`frontend`:** Runs via Vite `npm run dev` with instant Hot Module Reloading (HMR) on `localhost:5173`.
3. **WSL 2 Integration:**
   * When developing in WSL 2 (Ubuntu), enable Docker Desktop's **WSL Integration** (Settings → Resources → WSL Integration) or install the native `docker.io` package to allow `exec-runner` access to Docker.
4. **Cloud Parity:**
   * Once validated locally, deployment to a GCP Compute Engine VM uses the exact same `compose.yaml` with production environment flags.
