# 04 — AI & Retrieval

Every AI capability sits behind a provider-agnostic interface so provider choice is an `.env` line, not an architecture commit.

## Provider abstraction

```java
interface KataGenerator {
    GenerationResult generate(GenerationRequest request);   // structured, schema-validated output
    GenerationResult critique(GenerationRequest original, ValidationResult failure); // repair loop
}

interface EmbeddingClient {              // phase 2 / M4
    float[] embed(String text);
}
```

Concrete adapters: `OpenAiCompatibleKataGenerator` (covers OpenAI/local Ollama/vLLM — all speak one HTTP dialect), `AnthropicKataGenerator`. Selected by `AI_PROVIDER=openai|anthropic`, keyed by `.env`. The Spring Boot `api` hosts this; no separate AI service ([02-architecture.md](02-architecture.md)).

## Mode 1 — UI-picked generation (M2)

1. User picks `language + category + theme + difficulty` in the UI.
2. `api` checks the per-user generation quota, creates `generation_job`.
3. Prompt builds from a taxonomy snapshot (category description, similar-suite counts) — the store informs quality *even in mode 1*.
4. Response must be schema-validated JSON: `{title, statement_md, starter_code, reference_solution, test_suite, tags[], category}` — reject and re-ask on violation (1 retry, then surface error).
5. Generated suite enters the [validation loop](#validation-loop) in the real sandbox.
6. Persist: suite tagged; kata published to the requesting workspace; suite enters the shared corpus for other users (visibility flag).

### Validation loop

Mandatory — see the [roadmap push-back](01-vision-and-roadmap.md#deliberate-push-backs-encoded-here).

```
suite = generate(...)
ref_result  = sandbox.run(reference_solution, suite)
assert ref_result passed
mutations   = apply(strategies: [constant_return, inverted_condition, off_by_one], reference_solution, max 3)
mut_results = sandbox.run(mutation, suite) for each mutation
assert all(mut_results failed)
on failure → critique(original, failure) → regenerate suite only (≤2 passes) → else status=DRAFT
```

A suite that passes reference *and* kills mutations is functionally discriminating. This is what makes the retrieval corpus trustworthy for mode 2.

**Mutation Strategy Simplification (Regex-Only):**
Rather than introducing heavy Abstract Syntax Tree (AST) parsers (like Babel or JavaParser), mutations are generated via deterministic regex string replacements directly in Spring Boot:
1. **Constant Return:** Replace the primary return statement (`return\s+[^;]+;` → `return null;` or `return 0;`).
2. **Inverted Condition:** Flip the first equality or boolean check (`===` → `!==` or `>` → `<`).
3. **Off-by-One:** Mutate boundary operators (`<` → `<=`).

If the AI-generated test suite fails to catch these obvious logic mutations, it is discarded or retried.

## Mode 2 — Agent with DB tools (M4)

From chat, user says e.g. *"give me a harder recursion kata touching string parsing"*.

- Agent loop hosted in `api`: model handles tool-calling until it decides to construct.
- Same validation loop gates construction.
- **Tool-call traces are streamed to the user as SSE** — transparency is a demo feature, not a debug one.

### Tool specification

| Tool | Signature | Returns | Notes |
|---|---|---|---|
| `search_test_suites` | `{language, category?, theme?, tags?, difficulty?, min_use_count?}`, limit 10 | suite summaries: id, title, statement excerpt, tag vector, pass-rate stats | lexical search in M3 (`tsvector`), semantic in M4 (pgvector) |
| `get_kata_details` | `{kata_id}` | full statement, reference solution summary, suite composition | full reference solution is fine: platform-generated, no user secrets |
| `list_categories` | `{}` | category + theme taxonomy with counts | anchors the model to real taxonomy |
| `save_kata_draft` | `{kata_payload}` | validation job id | routes construction through the standard loop |
| `get_marketplace_stats` | `{category}` | most/least exercised themes | steers novelty |

**Harness policy:** max 6 tool calls per generation job; tool sessions are per-request, no cross-request memory in v1.

## Retrieval design

Phase 1 (M3): exact + fuzzy lexical — Postgres `tsvector` on statements, exact tag matching, trigram `pg_trgm` fallback.
Phase 2 (M4): `pgvector` on `embedding(kata.statement)`; recall blend: semantic top-k ∪ tag hits, re-ranked by suite stats (pass-rate, use count, recency).

`search_test_suites` and the "find relevant coding tests" UI both call the **same repo-level search service** — one implementation, two entrypoints.

## Cost & abuse control

| Control | Setting (initial) |
|---|---|
| Per-user generation quota | 5/day, 1 concurrent |
| Schema-violation retries | 1 |
| Validation retries | ≤2 |
| Model tiering | generation → strong model; critique/embeddings → cheap model |
| Job persistence | every call logged with token counts on `generation_jobs` |

Provider choice deferred = genuine [locked decision](../README.md#scope-decisions-locked); the interface force-decouples it.
