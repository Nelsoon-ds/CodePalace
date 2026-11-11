# 07 — Frontend

React + Vite. Editor: **https://github.com/petersolopov/yace**. Analysis below; this is the [locked stack](../README.md#scope-decisions-locked) recommendation encoded.


## Screens

| Screen | Contents | Ships |
|---|---|---|
| Login / Register | email + password forms | M1 |
| Workspaces | grid of user workspaces | M1 |
| Workspace shell | left: kata description & session list; center: **single active Monaco editor**; right/bottom: console results & run history | M1 (single-file editor) |
| Generation picker | language/category/theme/difficulty modal | M2 |
| Agent chat | full width, tool traces inline | M4 |

## State & data flow

- **TanStack Query** for all REST (workspaces, sessions, katas, runs).
- **SSE** (`EventSource`) per active run: reuses native reconnect; events per [API](05-data-model-api.md#sse-event-envelope): optimistic console assertions as they land.
- **Zustand** only for ephemeral editor state (active tab, terminal state); persistence stays server-authoritative.
- **Autosave:** debounced `PUT /sessions/{id}/draft` at 800ms idle + on blur; editor value hydrated from last submission *or* draft on load.
- **Auth:** JWT in memory + silent-refresh attempt on 401 → redirect login; no cookies/local storage of tokens longer than the session.

## Single-File Editor UX (Locked Simplification)

Rather than emulating a multi-file IDE with tabs and a file tree, CodePalace adopts a **LeetCode / Codewars-style single-file editor per kata**:

```mermaid
flowchart LR
    WS[Workspace Shell] --> S1[Active Session: Kata]
    S1 --> E[Single Yace Instance]
    E -->|debounced autosave 800ms| API[api draft endpoint]
    E -->|Submit button| API2[api submissions]
    SSO[SSE run events] --> CONSOLE[Results console]
```

- **One editor instance per active session:** Mounts the session's code (e.g. `solution.js`) directly.
- **Zero multi-tab complexity:** Switching to a different kata simply re-hydrates the single editor with that kata's code. This eliminates multi-model memory leaks and tab-state synchronization headaches in React.
- **Undo stack:** Handled natively by the Yace instance for the currently active kata.

## Results console UX

- Assertion list rows: name, status icon, duration; failed rows expandable to diff.
- **Users never see suite code** — consistent with the [privacy invariant](05-data-model-api.md#hidden-suite-privacy-invariant).
- Timeout / OOM states have distinct, named messages ([03](03-execution-sandbox.md#run-lifecycle-state-machine)) — a timeout is never "compilation error".

## Later phases

M4 multi-file tabs and file tree ride on the M1 schema (sessions already host multiple submissions; kata files arrive as a `kata_files` extension if workspace productization demands it, but editor tabs stay session-local in v1).
