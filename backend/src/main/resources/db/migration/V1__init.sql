CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspaces (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_name text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX workspaces_user_id_idx ON workspaces (user_id);


CREATE TABLE katas (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title text NOT NULL,
    statement_md text NOT NULL,
    difficulty text NOT NULL,
    programming_language text NOT NULL CHECK (
        programming_language IN ('js', 'java')
    ),
    starter_code text NOT NULL,
    reference_solution text,
    status text NOT NULL DEFAULT 'draft' CHECK (
        status IN ('draft', 'published', 'rejected')
    ),
    visibility text NOT NULL DEFAULT 'private',
    created_by text NOT NULL CHECK (
        created_by IN ('system', 'generation_job', 'user')
    ),
    created_at timestamptz NOT NULL DEFAULT now()
);


CREATE TABLE sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id uuid NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    kata_id uuid REFERENCES katas (id) ON DELETE SET NULL,
    programming_language text NOT NULL CHECK (
        programming_language IN ('js', 'java')
    ),
    status text NOT NULL DEFAULT 'active' CHECK (
        status IN ('active', 'archived')
    ),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX sessions_workspace_id_idx ON sessions (workspace_id);
CREATE INDEX sessions_kata_id_idx ON sessions (kata_id);

CREATE TABLE test_suites (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kata_id uuid NOT NULL REFERENCES katas (id) ON DELETE CASCADE,
    programming_language text NOT NULL CHECK (language IN ('js', 'java')),
    framework text NOT NULL CHECK (framework IN ('vitest', 'junit5')),
    code text NOT NULL,
    suite_hash text NOT NULL,
    mutation_strategies jsonb NOT NULL DEFAULT '[]'::jsonb,
    validation_stats jsonb NOT NULL DEFAULT '{}'::jsonb,
    use_count integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX test_suites_kata_id_idx ON test_suites (kata_id);

CREATE TABLE submissions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    code text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX submissions_session_id_idx ON submissions (session_id);

CREATE TABLE runs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id uuid NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    suite_id uuid NOT NULL REFERENCES test_suites (id),
    status text NOT NULL DEFAULT 'QUEUED',
    exit_code integer,
    duration_ms integer,
    results_jsonb jsonb,
    output_tail text,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX runs_submission_id_idx ON runs (submission_id);
CREATE INDEX runs_suite_id_idx ON runs (suite_id);

CREATE TABLE jobs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id uuid REFERENCES runs (id) ON DELETE CASCADE,
    work_type text NOT NULL,
    status text NOT NULL DEFAULT 'QUEUED',
    attempts integer NOT NULL DEFAULT 0,
    claimed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX jobs_run_id_idx ON jobs (run_id);
-- Partial index: the startup drain only ever scans queued rows
CREATE INDEX jobs_queued_idx ON jobs (created_at) WHERE status = 'QUEUED';
