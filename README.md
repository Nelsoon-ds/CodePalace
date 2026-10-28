# CodePalace

CodePalace is an AI-driven coding-kata training platform. Users get an IDE-like workspace where they solve katas in **Java** or **JavaScript**. Katas and their (hidden) test suites are AI-generated, validated, tagged with categories/themes, and persisted — so requests for relevant coding tests can either be picked in the UI or satisfied by an AI agent that retrieves tagged suites from the database before constructing new ones.

## Core ideas

- **Editor per session** – a Monaco-based editor inside a workspace; code autosaves, submissions run against hidden suites.
- **Sandboxed execution** – every run happens in an *ephemeral Docker container* (no network, resource-capped, read-only filesystem).
- **Test-suite store** – every generated suite is tagged with category + theme tags, making the DB itself a retrieval asset.
- **Two AI modes** – (1) UI-picked category → provider-agnostic generation; (2) later, an agent with DB tools that retrieves relevant suites before constructing.
- **Validation loop** – generated suites must pass on a reference solution *and* fail on mutations, or they never reach users.

## Scope decisions (locked)

| Area | Decision |
|---|---|
| Audience | Portfolio + small friendly demo users; email/password auth (Argon2 + JWT) |
| Execution isolation | Ephemeral container per run, `--network none`, CPU/memory/time caps |
| Language arc | JS-first (Node 22, vitest) → Java/JVM phase 2 (JUnit 5, warm-pool strategy) |
| AI | Provider-agnostic abstraction; both modes phased |
| Database | Postgres (decision matrix in [docs/06-devops.md](docs/06-devops.md)); pgvector in phase 2 |
| Hosting | Single VM + docker compose (services: `api`, `exec-runner`, `db`, `frontend`, `nginx`) |
| Frontend | React + Vite + Monaco editor (tradeoff analysis in [docs/07-frontend.md](docs/07-frontend.md)) |
| Backend | Spring Boot (Java), matches existing experience |
| Repo layout | Monorepo, English docs |

## Documentation

| Doc | Contents |
|---|---|
| [01-vision-and-roadmap.md](docs/01-vision-and-roadmap.md) | Goals, non-goals, milestones M0–M4 |
| [02-architecture.md](docs/02-architecture.md) | Services, request flows, sequence diagrams |
| [03-execution-sandbox.md](docs/03-execution-sandbox.md) | Threat model, container lifecycle, JVM notes, SSE streaming |
| [04-ai-and-retrieval.md](docs/04-ai-and-retrieval.md) | Provider abstraction, both generation modes, validation loop, tool-call spec |
| [05-data-model-api.md](docs/05-data-model-api.md) | ER model, hidden-suite privacy invariant, REST/SSE endpoint draft |
| [06-devops.md](docs/06-devops.md) | Monorepo layout, compose topology, DB decision matrix, VM sizing, CI |
| [07-frontend.md](docs/07-frontend.md) | Monaco vs CodeMirror, workspace shell, state management |

## Status

Planning phase. Implementation milestones start at M0 (see [vision & roadmap](docs/01-vision-and-roadmap.md)).
