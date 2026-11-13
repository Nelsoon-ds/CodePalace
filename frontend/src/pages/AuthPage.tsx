import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { authenticate } from "../api/hooks";
import { useAuth } from "../auth/AuthContext";
import styles from "../styles/Auth.module.css";

export function AuthPage({ mode }: { mode: "login" | "register" }) {
  const { authenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const register = mode === "register";
  const mutation = useMutation({
    mutationFn: () =>
      authenticate(register ? "/api/auth/register" : "/api/auth/login", email, password),
    onSuccess: () => {
      const next = searchParams.get("next");
      const destination =
        next?.startsWith("/") && !next.startsWith("//") && !next.includes("\\")
          ? next
          : "/workspaces";
      navigate(destination, { replace: true });
    },
  });

  if (authenticated) return <Navigate to="/workspaces" replace />;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate();
  }

  return (
    <main className={styles.page}>
      <div className={styles.ambient} />
      <header className={styles.topbar}>
        <Link className={styles.brand} to="/">
          <span className={styles.brandIcon}>C</span> codepalace
        </Link>
        <span className={styles.topbarNote}>A quiet place to get better.</span>
      </header>
      <section className={styles.panel}>
        <div className={styles.panelMark}>01 / YOUR PRACTICE</div>
        <h1>{register ? "Make room to grow." : "Welcome back."}</h1>
        <p className={styles.subhead}>
          {register
            ? "Build a steady practice, one thoughtful problem at a time."
            : "Your next small win starts right here."}
        </p>
        <form className={styles.form} onSubmit={submit}>
          <label htmlFor="email">Email address</label>
          <input
            autoComplete="email"
            id="email"
            name="email"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
            required
            type="email"
            value={email}
          />
          <label htmlFor="password">Password</label>
          <input
            autoComplete={register ? "new-password" : "current-password"}
            id="password"
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="At least 8 characters"
            required
            type="password"
            value={password}
          />
          {mutation.error && <p className={styles.error}>{mutation.error.message}</p>}
          <button className={styles.primaryButton} disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Please wait…" : register ? "Create your account" : "Sign in"}
            {!mutation.isPending && <span>↗</span>}
          </button>
        </form>
        <div className={styles.formFoot}>
          <span>{register ? "Already practicing here?" : "New to CodePalace?"}</span>
          <Link to={register ? "/login" : "/register"}>
            {register ? "Sign in" : "Create account"}
          </Link>
        </div>
      </section>
      <footer className={styles.footer}>
        <span>CODEPALACE · PRACTICE WITH PURPOSE</span>
        <span>01 — 03</span>
      </footer>
    </main>
  );
}
