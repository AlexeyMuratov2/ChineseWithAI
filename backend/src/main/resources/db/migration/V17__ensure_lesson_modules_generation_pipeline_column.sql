ALTER TABLE lesson_modules
    ADD COLUMN IF NOT EXISTS generation_pipeline_key VARCHAR(120);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_lesson_modules_generation_pipeline_key_not_blank'
    ) THEN
        ALTER TABLE lesson_modules
            ADD CONSTRAINT chk_lesson_modules_generation_pipeline_key_not_blank
                CHECK (generation_pipeline_key IS NULL OR btrim(generation_pipeline_key) <> '');
    END IF;
END $$;
