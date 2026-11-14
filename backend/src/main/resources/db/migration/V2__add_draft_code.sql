-- The Session entity maps the autosaved editor draft, but V1 never created the column.
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS draft_code text;
