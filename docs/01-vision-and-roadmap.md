# 01 — Vision & Roadmap

## Problem statement

Practicing code katas is still mostly: pick a static curated problem, write code, check against prewritten tests. CodePalace makes the *kata itself* dynamic: an AI generates a fresh, themed kata with a validated hidden test suite, persists it tagged by category/theme, and an agent can later retrieve and build on that corpus instead of regenerating blindly.

## Audience

| Who | Why it matters | Consequence |
|---|---|---|
| You (primary) | Portfolio piece + personal training tool | Solo dev, velocity over ceremony |
| Small demo users (secondary) | Shown to peers/employers | Needs real (lightweight) accounts, but no heavy abuse handling |

**Consequence:** email + password auth with Argon2 hashes and JWT. No OAuth providers, no email verification ceremony at MVP. Rate-limit the AI endpoints (they cost money) — see [04-ai-and-retrieval.md](04-ai-and-retrieval.md).

## Goals

1. A user opens a workspace, gets a code editor, and submits solutions that run in a real sandbox.
2. Generated kata suites are hidden from users; only test *results* are visible.
3. Every suite is persisted and tagged with category + theme so it is retrospect searchable.
4. AI operates in two modes (phased): UI-picked generation first, agent-with-DB-tools later.
5. Single-VM deployment with docker compose; cheap to run.

## Non-goals (v1)

- Multi-node scaling, horizontal sandbox fleets
- More languages (Java + JavaScript only)
- Collaborative editing / real-time multiuser sessions
- Ace/Cloud9-grade IDE features at MVP (multi-file tabs, refactorings) — the workspace *schema* supports them, the UI ships IDE-lite first

## Deliberate push-backs encoded here

1. **Workspace-first schema, IDE-lite UI.** The chosen "full IDE workspace" product shape is risky as an MVP. The data model is workspace-rooted from M1, but the MVP UI is a workspace shell with one active session/editor + chat. Multi-file workspace UI arrives in M4.
2. **The AI validation loop is mandatory.** A generated suite ships only if: it passes on the AI's own reference solution, *and* it fails on ≤5 mutation variants of that solution. Suites failing validation are retried (n≤2) then parked as `DRAFT`. This keeps the retrieval corpus from rotting — it is the foundation of mode 2, so it is not optional.

## Milestones

### M0 — Scaffold (repo bootstrap)
- Monorepo layout per [06-devops.md](06-devops.md); compose skeleton with `api`, `db`; CI pipeline; this doc set kept up to date as milestones land.

### M1 — Foundation: auth, workspace, editor, JS execution
- `api`: register/login (Argon2 + JWT), workspaces, sessions; autosave endpoint.
- `exec-runner`: spawn `codepalace/runner-node:22` container per run; vitest + JSON reporter; SSE results back through `api`.
- `frontend`: login, workspace list, workspace shell, Monaco editor per session, console-style results panel.
- Seeded (hand-written) JS katas so the loop works end-to-end before AI is involved.
- **Exit criteria:** demo user runs JS code and sees assertion results in <3s feel, runs isolated and capped.

### M2 — AI generation, mode 1 + validation loop
- Provider-agnostic `KataGenerator` interface; first concrete provider behind an API key in `.env`.
- Generation produces: statement, starter code, reference solution, hidden test suite, tags/category.
- Validation loop runs in the *real* sandbox (reuses M1 runner).
- Generation jobs persisted with token/cost accounting; per-user daily quota.
- **Exit criteria:** UI-picked category → published validated kata, or transparent `DRAFT`.

### M3 — Corpus & discovery
- Categories + free-form tags with keyword search (`tsvector`); "find relevant coding tests" endpoint backed by the store.
- Results streaming polish, run history in sessions.
- **Exit criteria:** UI can query the suite store by taxonomy and browse hits.

### M4 — Agent mode + Java + semantics
- Agent loop with DB tool calls ([tool spec](04-ai-and-retrieval.md#tool-specification)): retrieval then construction; tool-call traces surfaced to the user as SSE events.
- Java exec: `codepalace/runner-java:25`, JUnit 5, hidden-suite injection; evaluate cold start (~1–2s JVM boot inside 30s cap) and only invest in warm-pooling if needed.
- pgvector embeddings over kata statements for the semantic retrieval tool.
- Multi-file workspace UI (tabs, file tree) on the M1 schema.
- **Exit criteria:** natural-language request → agent retrieves suites, constructs novel kata, validated and published.
