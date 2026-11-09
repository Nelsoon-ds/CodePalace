-- Dev test data only. Clears existing users and katas, and everything linked to them.
-- Not in db/migration, so Flyway does not run it. Load manually with psql -f.
TRUNCATE users, katas CASCADE;

DO $$
DECLARE
  user_id uuid;
  workspace_id uuid;
  kata_id uuid;
  session_id uuid;
  good_submission_id uuid;
  bad_submission_id uuid;
  good_run_id uuid;
  bad_run_id uuid;
  suite_id uuid;
BEGIN
  INSERT INTO users (email, password_hash)
  VALUES ('test@example.com', 'not-a-real-hash')
  RETURNING id INTO user_id;

  INSERT INTO workspaces (user_id, user_name)
  VALUES (user_id, 'test-user')
  RETURNING id INTO workspace_id;

  INSERT INTO katas (title, statement_md, difficulty, programming_language, starter_code, created_by, status)
  VALUES ('Reverse a string', 'Write reverse(s) that returns s reversed.', 'easy', 'js',
    $$export function reverse(s) {

}
$$, 'system', 'published')
  RETURNING id INTO kata_id;

  INSERT INTO test_suites (kata_id, programming_language, framework, code, suite_hash)
  VALUES (kata_id, 'js', 'vitest',
    $$import { test, expect } from "vitest";
import { reverse } from "../src/solution.mjs";

test("reverses a string", () => {
  expect(reverse("abc")).toBe("cba");
});

test("handles empty string", () => {
  expect(reverse("")).toBe("");
});
$$, 'test-hash-1')
  RETURNING id INTO suite_id;

  INSERT INTO sessions (workspace_id, kata_id, programming_language)
  VALUES (workspace_id, kata_id, 'js')
  RETURNING id INTO session_id;

  INSERT INTO submissions (session_id, code)
  VALUES (session_id, $$export function reverse(s) { return s.split("").reverse().join(""); }$$)
  RETURNING id INTO good_submission_id;

  INSERT INTO submissions (session_id, code)
  VALUES (session_id, $$export function reverse(s) { return s; }$$)
  RETURNING id INTO bad_submission_id;

  INSERT INTO runs (submission_id, suite_id)
  VALUES (good_submission_id, suite_id)
  RETURNING id INTO good_run_id;

  INSERT INTO runs (submission_id, suite_id)
  VALUES (bad_submission_id, suite_id)
  RETURNING id INTO bad_run_id;

  INSERT INTO jobs (run_id, work_type) VALUES
    (good_run_id, 'run_tests'),
    (bad_run_id, 'run_tests');
END $$;

-- Shows each run id with its code. The "return s.split" run should pass, the "return s;" run should fail.
SELECT r.id AS run_id, s.code
FROM runs r
JOIN submissions s ON s.id = r.submission_id;
