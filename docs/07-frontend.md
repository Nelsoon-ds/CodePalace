# 07 — Frontend

React + Vite + Tailwind. Editor: **Monaco**. Analysis below; this is the [locked stack](../README.md#scope-decisions-locked) recommendation encoded.

## Monaco vs CodeMirror (decision: Monaco)

| Criterion | Monaco | CodeMirror 6 |
|---|---|---|
| Editor feel for Java/JS | VS Code-grade, full syntax + basic IntelliSense both | excellent, lighter weight |
| Diff view (kata reference diffing later) | built-in `createDiffEditor` | manual |
| React integration | `@monaco-editor/react` mature | solid |
| Bundle size | ~1.5-2MB loaded async | 300-500KB |
| Multi-file tabs + workspace polish (M4) | matches VS Code look — the product *is* a mini-IDE | cleaner minimal look |

Monaco's weight cost is irrelevant here (kata workspaces are heavy-use, not mobile-first), and the diff editor + IDE-shaped UI directly serve M4. Verdict: Monaco.

## Screens

| Screen | Contents | Ships |
|---|---|---|
| Login / Register | email + password forms | M1 |
| Workspaces | grid of user workspaces | M1 |
| Workspace shell | left: session list; center: **Monaco editor per session**; right: tabs (`Chat`, `Runs`); bottom: results console | M1 (single active session) → M4 (multi-tab) |
| Generation picker | language/category/theme/difficulty | M2 |
| Agent chat | full width, tool traces inline | M4 |

## State & data flow

- **TanStack Query** for all REST (workspaces, sessions, katas, runs).
- **SSE** (`EventSource`) per active run: reuses native reconnect; events per [API](05-data-model-api.md#sse-event-envelope): optimistic console assertions as they land.
- **Zustand** only for ephemeral editor state (active tab, terminal state); persistence stays server-authoritative.
- **Autosave:** debounced `PUT /sessions/{id}/draft` at 800ms idle + on blur; editor value hydrated from last submission *or* draft on load.
- **Auth:** JWT in memory + silent-refresh attempt on 401 → redirect login; no cookies/local storage of tokens longer than the session.

## Editor-per-session wiring

```mermaid
flowchart LR
    WS[Workspace shell] --> S1[Session tab: kata A]
    WS --> S2[Session tab: kata B, later]
    S1 --> E[Monaco instance]
    E -->|debounced autosave| API[api draft endpoint]
    E -->|Submit| API2[api submissions]
    SSO[SSE run events] --> CONSOLE[Results console]
```

One Monaco instance per active session; instances are kept-alive components mounted per open session tab so their undo stacks survive tab switches.

## Results console UX

- Assertion list rows: name, status icon, duration; failed rows expandable to diff.
- **Users never see suite code** — consistent with the [privacy invariant](05-data-model-api.md#hidden-suite-privacy-invariant).
- Timeout / OOM states have distinct, named messages ([03](03-execution-sandbox.md#run-lifecycle-state-machine)) — a timeout is never "compilation error".

## Later phases

M4 multi-file tabs and file tree ride on the M1 schema (sessions already host multiple submissions; kata files arrive as a `kata_files` extension if workspace productization demands it, but editor tabs stay session-local in v1).
