ALTER TABLE agent_sessions
    ADD COLUMN system_prompt_appendix TEXT;

CREATE TABLE lesson_modules (
    module_key VARCHAR(120) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    system_prompt_appendix TEXT NOT NULL,
    schema_version INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_lesson_modules_module_key_not_blank
        CHECK (btrim(module_key) <> ''),
    CONSTRAINT chk_lesson_modules_display_name_not_blank
        CHECK (btrim(display_name) <> ''),
    CONSTRAINT chk_lesson_modules_schema_version_positive
        CHECK (schema_version > 0)
);

CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    module_key VARCHAR(120) NULL,
    source_draft_id UUID NULL,
    generator_session_id UUID NULL,
    title VARCHAR(160) NOT NULL,
    study_language VARCHAR(35) NOT NULL,
    explanation_language VARCHAR(35) NOT NULL,
    translation_language VARCHAR(35) NOT NULL,
    content_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lessons_owner_id
        FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_lessons_module_key
        FOREIGN KEY (module_key) REFERENCES lesson_modules (module_key) ON DELETE SET NULL,
    CONSTRAINT fk_lessons_source_draft_id
        FOREIGN KEY (source_draft_id) REFERENCES lesson_drafts (id) ON DELETE SET NULL,
    CONSTRAINT fk_lessons_generator_session_id
        FOREIGN KEY (generator_session_id) REFERENCES agent_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_lessons_title_not_blank
        CHECK (btrim(title) <> ''),
    CONSTRAINT chk_lessons_study_language_not_blank
        CHECK (btrim(study_language) <> ''),
    CONSTRAINT chk_lessons_explanation_language_not_blank
        CHECK (btrim(explanation_language) <> ''),
    CONSTRAINT chk_lessons_translation_language_not_blank
        CHECK (btrim(translation_language) <> '')
);

CREATE INDEX idx_lessons_owner_updated_at ON lessons (owner_id, updated_at DESC);

INSERT INTO lesson_modules (
    module_key,
    display_name,
    system_prompt_appendix,
    schema_version,
    is_active,
    created_at,
    updated_at
) VALUES (
    'TestModule',
    'TestModule',
    'You generate test lessons from one text note source. The source contains a few new words and one short Chinese reading text. Keep the Chinese reading text in Chinese. Use explanationLanguage for titles and explanations. Use translationLanguage for translations.',
    1,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (module_key) DO NOTHING;

INSERT INTO agent_profiles (
    profile_key,
    display_name,
    system_prompt,
    model_key,
    context_builder_key,
    allowed_tools_json,
    execution_policy_json,
    memory_policy_json,
    output_contract_json,
    is_visible,
    created_at,
    updated_at
) VALUES (
    'lesson-generator:v1',
    'Lesson Generator v1',
    'You are a deterministic lesson generation agent. Produce exactly one lesson JSON object that matches the required contract and lesson-module instructions. Do not add markdown fences or commentary.',
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":4}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":8}',
    '{"requiredFields":{"schemaVersion":"number","moduleKey":"string","title":"string","studyLanguage":"string","explanationLanguage":"string","translationLanguage":"string","newWords":"array","sections":"array"}}',
    FALSE,
    NOW(),
    NOW()
)
ON CONFLICT (profile_key) DO NOTHING;
