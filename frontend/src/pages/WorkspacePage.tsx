import { useEffect, useMemo, useRef, useState } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { useKatas, useRun, useSession, useSessionMutations, useWorkspaces } from "../api/hooks";
import type { Kata, RunEvent, RunResult, Session } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { CodeEditor } from "../components/CodeEditor";
import { ResultsConsole } from "../components/ResultsConsole";
import { useRunStream } from "../components/RunStream";
import { useUiStore } from "../state/uiStore";
import styles from "../styles/Workspace.module.css";

function arrayFrom<T>(value: T[] | undefined) {
  return value ?? [];
}

function sessionCode(session?: Session) {
  const draft = typeof session?.draft === "string" ? session.draft : session?.draft?.code;
  return session?.draftCode ?? draft ?? session?.latestSubmission?.code ??
    session?.submissions?.[session.submissions.length - 1]?.code ?? session?.kata?.starterCode ?? "";
}

function sessionLabel(session: Session) {
  return session.title ?? session.name ?? "Practice session";
}

export function WorkspacePage() {
  const { authenticated } = useAuth();
  const navigate = useNavigate();
  const { workspaceId, sessionId } = useParams();
  const workspaceQuery = useWorkspaces();
  const workspace = workspaceQuery.data?.find((item) => item.id === workspaceId);
  const sessions = arrayFrom(workspace?.sessions);
  const sessionQuery = useSession(sessionId);
  const session = sessionQuery.data;
  const katasQuery = useKatas();
  const katas = katasQuery.data ?? [];
  const sessionMutations = useSessionMutations(sessionId);
  const runState = useState<{ runId: string; result?: RunResult }>();
  const [activeRun, setActiveRun] = runState;
  const { events, streamState, error: streamError } = useRunStream(activeRun?.runId);
  const runQuery = useRun(activeRun?.runId);
  const setSaveState = useUiStore((state) => state.setSaveState);
  const saveState = useUiStore((state) => state.saveState);
  const consoleOpen = useUiStore((state) => state.consoleOpen);
  const toggleConsole = useUiStore((state) => state.toggleConsole);
  const [codeValue, setCodeValue] = useState("");
  const [selectedKata, setSelectedKata] = useState("");
  const [sidebarTab, setSidebarTab] = useState<"sessions" | "katas">("sessions");
  const [editorError, setEditorError] = useState("");
  const saveTimer = useRef<ReturnType<typeof setTimeout>>();
  const saveRequest = useRef<(targetSessionId: string, value: string) => Promise<void>>(async () => undefined);
  const codeRef = useRef(codeValue);
  const hydratedSession = useRef<string>();
  const lastSavedCode = useRef("");

  useEffect(() => {
    if (!sessionId || !session) return;
    if (hydratedSession.current !== sessionId) {
      hydratedSession.current = sessionId;
      const next = sessionCode(session);
      codeRef.current = next;
      lastSavedCode.current = next;
      setCodeValue(next);
      setActiveRun(undefined);
      setEditorError("");
      setSaveState("saved");
    }
  }, [sessionId, session, setActiveRun, setSaveState]);

  const saveCode = async (targetSessionId: string, value: string) => {
    if (value === lastSavedCode.current) {
      setSaveState("saved");
      return;
    }
    setSaveState("saving");
    try {
      await sessionMutations.saveDraft.mutateAsync({ targetSessionId, code: value });
      if (targetSessionId === sessionId) {
        lastSavedCode.current = value;
        setSaveState("saved");
        setEditorError("");
      }
    } catch (error) {
      if (targetSessionId === sessionId) {
        setSaveState("error");
        setEditorError(error instanceof Error ? error.message : "Draft could not be saved.");
      }
    }
  };
  saveRequest.current = (targetSessionId, value) => saveCode(targetSessionId, value);

  useEffect(() => {
    if (!sessionId || !session || codeValue === lastSavedCode.current) return;
    setSaveState("unsaved");
    if (saveTimer.current) clearTimeout(saveTimer.current);
    const targetSessionId = sessionId;
    saveTimer.current = setTimeout(() => void saveRequest.current(targetSessionId, codeRef.current), 800);
    return () => {
      if (saveTimer.current) clearTimeout(saveTimer.current);
    };
  }, [codeValue, session, sessionId, setSaveState]);

  useEffect(() => {
    const targetSessionId = sessionId;
    return () => {
      if (saveTimer.current) clearTimeout(saveTimer.current);
      if (targetSessionId && codeRef.current !== lastSavedCode.current) {
        void saveRequest.current(targetSessionId, codeRef.current);
      }
    };
  }, [sessionId]);

  const kata = useMemo<Kata | undefined>(
    () => session?.kata ?? katas.find((item) => item.id === session?.kataId),
    [katas, session],
  );

 // if (!authenticated) return <Navigate to="/login" replace />;
  if (workspaceQuery.isLoading) return <div className={styles.loadingPage}>Opening workspace…</div>;
  if (workspaceQuery.error) return <div className={styles.loadingPage}>Could not load workspaces: {workspaceQuery.error.message}</div>;
  if (!workspace) return <Navigate to="/workspaces" replace />;

  async function startSession() {
    if (!workspaceId || !selectedKata) return;
    try {
      const created = await sessionMutations.createSession.mutateAsync({ workspaceId, kataId: selectedKata });
      if (!created.id) throw new Error("The session endpoint returned no session ID.");
      navigate(`/workspaces/${workspaceId}/sessions/${created.id}`);
    } catch (error) {
      setEditorError(error instanceof Error ? error.message : "Could not start this kata.");
    }
  }

  async function runCode() {
    if (!sessionId || !codeValue.trim()) return;
    setEditorError("");
    try {
      const result = await sessionMutations.submit.mutateAsync({
        sessionId,
        code: codeValue,
      });
      const runId = result.runId ?? result.id;
      if (!runId) throw new Error("The submission response did not include a run ID.");
      setActiveRun({ runId, result });
    } catch (error) {
      setEditorError(error instanceof Error ? error.message : "Submission failed.");
    }
  }

  const language = session?.language ?? kata?.language ?? "javascript";
  const canEdit = Boolean(sessionId && session);

  return (
    <main className={styles.workspace}>
      <header className={styles.topbar}>
        <Link className={styles.brand} to="/workspaces"><span className={styles.brandIcon}>C</span> codepalace</Link>
        <div className={styles.breadcrumb}>
          <Link to="/workspaces">Spaces</Link><span>/</span><strong>{workspace.name}</strong>
          {session && <><span>/</span><span>{sessionLabel(session)}</span></>}
        </div>
        <div className={styles.topActions}>
          <span className={styles.saveIndicator}><i data-state={saveState} />{saveStateLabel(saveState)}</span>
          <button className={styles.runButton} disabled={!canEdit || sessionMutations.submit.isPending} onClick={() => void runCode()} type="button">
            <span>▶</span>{sessionMutations.submit.isPending ? "Running…" : "Run tests"}
          </button>
        </div>
      </header>
      <div className={styles.layout}>
        <aside className={styles.sidebar}>
          <div className={styles.sidebarTitle}>
            <span>WORKSPACE</span><button aria-label="Refresh workspace" onClick={() => void workspaceQuery.refetch()} type="button">↻</button>
          </div>
          <div className={styles.sideTabs}>
            <button className={sidebarTab === "sessions" ? styles.selectedTab : ""} onClick={() => setSidebarTab("sessions")} type="button">Sessions</button>
            <button className={sidebarTab === "katas" ? styles.selectedTab : ""} onClick={() => setSidebarTab("katas")} type="button">Katas</button>
          </div>
          {sidebarTab === "sessions" ? (
            <div className={styles.sideContent}>
              <p className={styles.sectionLabel}>YOUR SESSIONS <span>{sessions.length}</span></p>
              {sessions.map((item) => (
                <Link
                  className={`${styles.sessionItem} ${item.id === sessionId ? styles.activeSession : ""}`}
                  key={item.id}
                  to={`/workspaces/${workspaceId}/sessions/${item.id}`}
                >
                  <span className={styles.sessionIcon}>⌘</span>
                  <span><strong>{item.title ?? item.name ?? item.kataId ?? "Practice session"}</strong><small>{(item.language ?? "javascript").toUpperCase()}</small></span>
                </Link>
              ))}
              {sessions.length === 0 && <p className={styles.smallHint}>No sessions yet. Choose a kata to begin.</p>}
              <button className={styles.addSession} onClick={() => setSidebarTab("katas")} type="button"><span>＋</span> Start a kata</button>
            </div>
          ) : (
            <div className={styles.sideContent}>
              <p className={styles.sectionLabel}>KATA LIBRARY <span>{katas.length}</span></p>
              {katasQuery.isLoading ? <p className={styles.smallHint}>Loading katas…</p> :
                katasQuery.error ? <p className={styles.inlineError}>{katasQuery.error.message}</p> :
                  katas.map((item) => (
                    <button className={`${styles.kataItem} ${selectedKata === item.id ? styles.kataSelected : ""}`} key={item.id} onClick={() => setSelectedKata(item.id)} type="button">
                      <strong>{item.title}</strong>
                      <small>{item.difficulty ?? "Practice"} · {(item.language ?? "javascript").toUpperCase()}</small>
                    </button>
                  ))}
              {selectedKata && (
                <button className={styles.addSession} disabled={sessionMutations.createSession.isPending} onClick={() => void startSession()} type="button">
                  {sessionMutations.createSession.isPending ? "Starting…" : "Start selected kata"} <span>↗</span>
                </button>
              )}
            </div>
          )}
          <div className={styles.sidebarFoot}>CODEPALACE <span>BUILD 01</span></div>
        </aside>
        <section className={styles.mainColumn}>
          {session ? (
            <>
              <div className={styles.kataHeader}>
                <div className={styles.kataHeading}>
                  <div className={styles.kataMeta}><span>{String(kata?.difficulty ?? "PRACTICE").toUpperCase()}</span><i />{String(kata?.category && typeof kata.category === "object" ? kata.category.name ?? "KATA" : kata?.category ?? "KATA").toUpperCase()}</div>
                  <h1>{kata?.title ?? sessionLabel(session)}</h1>
                  <p>{kata?.statementMd ?? kata?.description ?? "Write a solution that passes the assertions. Your work is saved as you go."}</p>
                </div>
                <span className={styles.languagePill}>{language}</span>
              </div>
              <div className={styles.editorBar}><span><i /> {session.name ?? "solution"}{language.toLowerCase().includes("java") ? ".java" : ".js"}</span><span>YACE EDITOR</span></div>
              <div className={styles.editorWrap}>
                {canEdit ? (
                  <CodeEditor
                    key={sessionId}
                    language={language}
                    onBlur={() => {
                      if (saveTimer.current) clearTimeout(saveTimer.current);
                      if (sessionId) void saveRequest.current(sessionId, codeRef.current);
                    }}
                    onChange={(next) => {
                      codeRef.current = next;
                      setCodeValue(next);
                    }}
                    value={kata?.starterCode || "No starter code"}
                  />
                ) : <p className={styles.smallHint}>Loading session…</p>}
              </div>
              {editorError && <p className={styles.editorError}>{editorError}</p>}
              <button className={styles.consoleToggle} onClick={toggleConsole} type="button">
                <span>{consoleOpen ? "⌄" : "⌃"} &nbsp; Results console</span><span>{activeRun ? "RUN ACTIVE" : "READY"}</span>
              </button>
              {consoleOpen && (
                <ResultsConsole
                  error={streamError ?? (runQuery.error instanceof Error ? runQuery.error.message : undefined)}
                  events={events as RunEvent[]}
                  run={runQuery.data ?? activeRun?.result}
                  runId={activeRun?.runId}
                  streamState={streamState}
                />
              )}
            </>
          ) : (
            <div className={styles.chooseKata}>
              <div className={styles.chooseGlyph}>✳</div>
              <p className={styles.kataMeta}>A GOOD PLACE TO BEGIN</p>
              <h1>Pick a kata.<br /><em>Find your rhythm.</em></h1>
              <p>Select a session from the left, or browse the kata library and start something new.</p>
              <button className={styles.runButton} onClick={() => setSidebarTab("katas")} type="button">Browse katas <span>↗</span></button>
              {sessionQuery.error && <p className={styles.inlineError}>{sessionQuery.error.message}</p>}
              {sessionId && sessionQuery.isLoading && <p className={styles.smallHint}>Loading session…</p>}
              {editorError && <p className={styles.inlineError}>{editorError}</p>}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

function saveStateLabel(state: string) {
  if (state === "saving") return "Saving";
  if (state === "unsaved") return "Unsaved";
  if (state === "error") return "Save failed";
  return "All changes saved";
}
