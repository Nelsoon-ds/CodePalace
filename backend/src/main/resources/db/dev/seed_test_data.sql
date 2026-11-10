-- Development fixtures for a fresh database; run once after V1__init.sql.
BEGIN;

-- 1. Accounts and a workspace for the first user.
insert into users (email, password_hash) values ('test@example.com', 'not-a-hash');
insert into users (email, password_hash) values ('test2@example.com', 'not-a-hash');
insert into users (email, password_hash) values ('test3@example.com', 'not-a-hash');

INSERT INTO workspaces (user_id, user_name) SELECT id, 'test-user' FROM users WHERE email = 'test@example.com';

-- 2. Shared problems. Named exports match the sandbox's solution.mjs imports.
INSERT INTO katas (title, statement_md, difficulty, programming_language, starter_code, reference_solution, status, visibility, created_by) VALUES
('Sum of Two Numbers', 'Write a function that takes two numbers and returns their sum.', 'easy', 'js',
'export function sum(a, b) {
  // Your code here
}
',
'export function sum(a, b) {
  return a + b;
}
', 'published', 'public', 'system'),
('Fibonacci Sequence', 'Write a function that returns the nth Fibonacci number.', 'hard', 'js',
'export function fibonacci(n) {
  // Your code here
}
',
'export function fibonacci(n) {
  if (n <= 1) return n;
  return fibonacci(n - 1) + fibonacci(n - 2);
}
', 'published', 'public', 'system');

-- 3. The first user's attempt context for Sum of Two Numbers.
INSERT INTO sessions (workspace_id, kata_id, programming_language, status) SELECT w.id, k.id, k.programming_language, 'active'
FROM workspaces w JOIN katas k ON k.title = 'Sum of Two Numbers' WHERE w.user_name = 'test-user';

-- 4. Hidden tests, uploaded by the runner as tests/suite.test.mjs.
-- Define the source once so suite_hash fingerprints the exact stored code.
-- Mutation/validation metadata and use_count keep their empty/zero defaults.
WITH suite_source AS (
    SELECT 'import { test, expect } from "vitest";
import { sum } from "../src/solution.mjs";

test("adds positive numbers", () => {
  expect(sum(2, 3)).toBe(5);
});

test("adds negative numbers", () => {
  expect(sum(-2, -3)).toBe(-5);
});

test("adds numbers with different signs", () => {
  expect(sum(-4, 7)).toBe(3);
  expect(sum(4, -7)).toBe(-3);
});

test("handles zero in either position", () => {
  expect(sum(0, 5)).toBe(5);
  expect(sum(5, 0)).toBe(5);
  expect(sum(0, 0)).toBe(0);
});

test("adds fractional numbers", () => {
  expect(sum(1.5, 2.25)).toBe(3.75);
});
' AS code
)
INSERT INTO test_suites (kata_id, programming_language, framework, code, suite_hash)
SELECT k.id, k.programming_language, 'vitest', source.code,
       encode(sha256(convert_to(source.code, 'UTF8')), 'hex')
FROM katas k CROSS JOIN suite_source source
WHERE k.title = 'Sum of Two Numbers';

-- 5. Save a passing solution, link it to a run, and queue that run.
-- RETURNING passes each newly generated ID to the next INSERT.
WITH seeded_submission AS (
    INSERT INTO submissions (session_id, code)
    SELECT s.id, 'export function sum(a, b) {
  return a + b;
}
'
    FROM sessions s
    JOIN workspaces w ON w.id = s.workspace_id
    JOIN users u ON u.id = w.user_id
    JOIN katas k ON k.id = s.kata_id
    WHERE u.email = 'test@example.com'
      AND k.title = 'Sum of Two Numbers'
    RETURNING id, session_id
), seeded_run AS (
    INSERT INTO runs (submission_id, suite_id, status)
    SELECT submission.id, suite.id, 'QUEUED'
    FROM seeded_submission submission
    JOIN sessions s ON s.id = submission.session_id
    JOIN test_suites suite ON suite.kata_id = s.kata_id
    RETURNING id
)
INSERT INTO jobs (run_id, work_type, status)
SELECT id, 'RUN_SUBMISSION', 'QUEUED'
FROM seeded_run
RETURNING id AS job_id, run_id;

-- Results stay NULL until the runner actually executes the queued run.
COMMIT;
