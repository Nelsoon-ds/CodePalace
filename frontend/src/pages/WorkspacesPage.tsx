import { useState, type FormEvent } from "react";
import { Link, Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useWorkspaceMutations, useWorkspaces } from "../api/hooks";
import styles from "../styles/Workspaces.module.css";

export function WorkspacesPage() {
  const { authenticated, logout } = useAuth();
  const workspaces = useWorkspaces();
  const { create, remove } = useWorkspaceMutations();
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");

  if (!authenticated) return <Navigate to="/login" replace />;

  function createWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    create.mutate(name.trim(), {
      onSuccess: () => {
        setName("");
        setShowCreate(false);
      },
    });
  }

  return (
    <main className={styles.page}>
      <header className={styles.nav}>
        <Link className={styles.brand} to="/workspaces">
          <span className={styles.brandIcon}>C</span> codepalace
        </Link>
        <button className={styles.logout} onClick={logout} type="button">Sign out ↗</button>
      </header>
      <section className={styles.content}>
        <div className={styles.intro}>
          <div>
            <p className={styles.kicker}>YOUR PRACTICE SPACE</p>
            <h1>Make a little progress.</h1>
            <p className={styles.lede}>A workspace for every thread of curiosity.</p>
          </div>
          <button className={styles.newButton} onClick={() => setShowCreate(true)} type="button">
            <span>＋</span> New workspace
          </button>
        </div>
        {workspaces.isLoading ? (
          <div className={styles.emptyState}>Loading your spaces…</div>
        ) : workspaces.error ? (
          <div className={styles.errorState}>{workspaces.error.message}</div>
        ) : workspaces.data?.length ? (
          <div className={styles.grid}>
            {workspaces.data.map((workspace, index) => (
              <article className={styles.card} key={workspace.id}>
                <div className={styles.cardTop}>
                  <span className={styles.cardIndex}>0{index + 1}</span>
                  <button
                    aria-label={`Delete ${workspace.name}`}
                    className={styles.deleteButton}
                    disabled={remove.isPending}
                    onClick={() => {
                      if (window.confirm(`Delete “${workspace.name}”?`)) remove.mutate(workspace.id);
                    }}
                    type="button"
                  >
                    ×
                  </button>
                </div>
                <Link className={styles.cardLink} to={`/workspaces/${workspace.id}`}>
                  <h2>{workspace.name}</h2>
                  <p>{workspace.sessions?.length ?? 0} practice sessions</p>
                  <span className={styles.arrow}>↗</span>
                </Link>
                <div className={styles.cardRule} />
                <span className={styles.cardFoot}>OPEN WORKSPACE</span>
              </article>
            ))}
            <button className={styles.addCard} onClick={() => setShowCreate(true)} type="button">
              <span>＋</span>
              <strong>Start another space</strong>
              <small>Keep a new idea close.</small>
            </button>
          </div>
        ) : (
          <div className={styles.emptyState}>
            <span className={styles.emptyGlyph}>✳</span>
            <h2>A fresh page.</h2>
            <p>Create a workspace to keep your coding practice in one place.</p>
            <button className={styles.newButton} onClick={() => setShowCreate(true)} type="button">Create a workspace</button>
          </div>
        )}
        {workspaces.error && (
          <button className={styles.retry} onClick={() => workspaces.refetch()} type="button">Try again</button>
        )}
        {remove.error && <p className={styles.errorState}>{remove.error.message}</p>}
      </section>
      {showCreate && (
        <div className={styles.modalBackdrop} onMouseDown={(event) => {
          if (event.target === event.currentTarget) setShowCreate(false);
        }}>
          <form className={styles.modal} onSubmit={createWorkspace}>
            <button className={styles.modalClose} onClick={() => setShowCreate(false)} type="button" aria-label="Close">×</button>
            <p className={styles.kicker}>NEW SPACE</p>
            <h2>What are you working on?</h2>
            <label htmlFor="workspace-name">Workspace name</label>
            <input id="workspace-name" maxLength={80} onChange={(event) => setName(event.target.value)} required value={name} autoFocus />
            {create.error && <p className={styles.errorState}>{create.error.message}</p>}
            <button className={styles.newButton} disabled={create.isPending} type="submit">
              {create.isPending ? "Creating…" : "Create workspace"} <span>↗</span>
            </button>
          </form>
        </div>
      )}
    </main>
  );
}
