-- The runner reports the duration as a fractional number of milliseconds, and the development
-- database was created with a double precision column. V1 declared it as an integer, which the
-- entity mapping rejected. Align both with double precision.
ALTER TABLE runs
    ALTER COLUMN duration_ms TYPE double precision;

-- The development database was created by Hibernate and therefore never received the CHECK
-- constraints that V1 declares. Fresh databases already have them, so only add the missing ones.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'katas_programming_language_check') THEN
        ALTER TABLE katas ADD CONSTRAINT katas_programming_language_check
            CHECK (programming_language IN ('js', 'java'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'katas_status_check') THEN
        ALTER TABLE katas ADD CONSTRAINT katas_status_check
            CHECK (status IN ('draft', 'published', 'rejected'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'katas_created_by_check') THEN
        ALTER TABLE katas ADD CONSTRAINT katas_created_by_check
            CHECK (created_by IN ('system', 'generation_job', 'user'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sessions_programming_language_check') THEN
        ALTER TABLE sessions ADD CONSTRAINT sessions_programming_language_check
            CHECK (programming_language IN ('js', 'java'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sessions_status_check') THEN
        ALTER TABLE sessions ADD CONSTRAINT sessions_status_check
            CHECK (status IN ('active', 'archived'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'test_suites_programming_language_check') THEN
        ALTER TABLE test_suites ADD CONSTRAINT test_suites_programming_language_check
            CHECK (programming_language IN ('js', 'java'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'test_suites_framework_check') THEN
        ALTER TABLE test_suites ADD CONSTRAINT test_suites_framework_check
            CHECK (framework IN ('vitest', 'junit5'));
    END IF;
END
$$;
