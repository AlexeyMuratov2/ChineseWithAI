UPDATE agent_pre_generation_workflows
SET steps_json = (
        SELECT COALESCE(jsonb_agg(step.value ORDER BY step.ordinality), '[]'::jsonb)::text
        FROM jsonb_array_elements(steps_json::jsonb) WITH ORDINALITY AS step(value, ordinality)
        WHERE step.value ->> 'stepKey' <> 'current-user-profile'
    ),
    updated_at = NOW()
WHERE steps_json::jsonb @> '[{"stepKey":"current-user-profile"}]'::jsonb;

ALTER TABLE lesson_drafts
    DROP CONSTRAINT IF EXISTS fk_lesson_drafts_owner_id;

DROP INDEX IF EXISTS idx_lesson_drafts_owner_updated_at;

ALTER TABLE lesson_drafts
    DROP COLUMN IF EXISTS owner_id;

ALTER TABLE agent_sessions
    DROP CONSTRAINT IF EXISTS fk_agent_sessions_owner_id;

DROP INDEX IF EXISTS idx_agent_sessions_owner_created_at;

ALTER TABLE agent_sessions
    DROP COLUMN IF EXISTS owner_id;

ALTER TABLE lessons
    DROP CONSTRAINT IF EXISTS fk_lessons_owner_id;

DROP INDEX IF EXISTS idx_lessons_owner_updated_at;

ALTER TABLE lessons
    DROP COLUMN IF EXISTS owner_id;

ALTER TABLE lesson_vocabulary_items
    DROP CONSTRAINT IF EXISTS fk_lesson_vocabulary_items_user_id;

DROP INDEX IF EXISTS idx_lesson_vocabulary_items_user_created_at;

ALTER TABLE lesson_vocabulary_items
    DROP COLUMN IF EXISTS user_id;

ALTER TABLE learner_vocabulary_progress
    DROP CONSTRAINT IF EXISTS fk_learner_vocabulary_progress_user_id;

DROP INDEX IF EXISTS uq_learner_vocabulary_progress_user_word;
DROP INDEX IF EXISTS idx_learner_vocabulary_progress_review_lookup;

ALTER TABLE learner_vocabulary_progress
    DROP COLUMN IF EXISTS user_id;

CREATE INDEX IF NOT EXISTS idx_learner_vocabulary_progress_review_lookup
    ON learner_vocabulary_progress (translation_language, status);

CREATE INDEX IF NOT EXISTS idx_learner_vocabulary_progress_word_lookup
    ON learner_vocabulary_progress (hanzi, pinyin, translation_language, updated_at DESC, id DESC);

-- Some databases reached Flyway v17 without ever creating lesson_generation_runs (e.g. drift or partial history).
-- V19 ensures these tables exist; skip owner_id cleanup when the table is absent.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'lesson_generation_runs'
    ) THEN
        ALTER TABLE lesson_generation_runs
            DROP CONSTRAINT IF EXISTS fk_lesson_generation_runs_owner_id;
        DROP INDEX IF EXISTS idx_lesson_generation_runs_owner_created_at;
        ALTER TABLE lesson_generation_runs
            DROP COLUMN IF EXISTS owner_id;
    END IF;
END $$;

DROP TABLE IF EXISTS app_user;
