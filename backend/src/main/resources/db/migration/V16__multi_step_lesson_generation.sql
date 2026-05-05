ALTER TABLE lesson_modules
    ADD COLUMN generation_pipeline_key VARCHAR(120);

ALTER TABLE lesson_modules
    ADD CONSTRAINT chk_lesson_modules_generation_pipeline_key_not_blank
        CHECK (generation_pipeline_key IS NULL OR btrim(generation_pipeline_key) <> '');

CREATE TABLE lesson_generation_runs (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    draft_id UUID NOT NULL,
    module_key VARCHAR(120) NOT NULL,
    pipeline_key VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    lesson_id UUID NULL,
    final_generator_session_id UUID NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lesson_generation_runs_owner_id
        FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_generation_runs_draft_id
        FOREIGN KEY (draft_id) REFERENCES lesson_drafts (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_generation_runs_module_key
        FOREIGN KEY (module_key) REFERENCES lesson_modules (module_key) ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_generation_runs_lesson_id
        FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE SET NULL,
    CONSTRAINT fk_lesson_generation_runs_final_generator_session_id
        FOREIGN KEY (final_generator_session_id) REFERENCES agent_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_lesson_generation_runs_pipeline_key_not_blank
        CHECK (btrim(pipeline_key) <> ''),
    CONSTRAINT chk_lesson_generation_runs_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_lesson_generation_runs_owner_created_at
    ON lesson_generation_runs (owner_id, created_at DESC);

CREATE TABLE lesson_generation_run_stages (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    stage_index INT NOT NULL,
    stage_key VARCHAR(120) NOT NULL,
    agent_session_id UUID NULL,
    status VARCHAR(30) NOT NULL,
    output_json TEXT NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lesson_generation_run_stages_run_id
        FOREIGN KEY (run_id) REFERENCES lesson_generation_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_generation_run_stages_agent_session_id
        FOREIGN KEY (agent_session_id) REFERENCES agent_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_lesson_generation_run_stages_stage_index_non_negative
        CHECK (stage_index >= 0),
    CONSTRAINT chk_lesson_generation_run_stages_stage_key_not_blank
        CHECK (btrim(stage_key) <> ''),
    CONSTRAINT chk_lesson_generation_run_stages_status
        CHECK (status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT uq_lesson_generation_run_stages_run_stage_index
        UNIQUE (run_id, stage_index),
    CONSTRAINT uq_lesson_generation_run_stages_run_stage_key
        UNIQUE (run_id, stage_key)
);

CREATE INDEX idx_lesson_generation_run_stages_run_stage_index
    ON lesson_generation_run_stages (run_id, stage_index);

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
    auto_repair_invalid_output_enabled,
    output_validation_strategy_key,
    is_visible,
    created_at,
    updated_at
) VALUES
(
    'lesson-stage:hsk5_v1_blueprint',
    'Lesson Stage HSK5 Blueprint',
    $$You create a compact planning artifact for an HSK 5 Chinese lesson. Return exactly one JSON object and no commentary. The artifact chooses new words, review words, grammar focus, tone, and goal before the final lesson is composed.$$,
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":2}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":4}',
    '{"requiredFields":{"title":"string","readingText":"string","newWords":"array","reviewWords":"array","grammarPoints":"array","lessonTone":"string","lessonGoal":"string"}}',
    FALSE,
    NULL,
    FALSE,
    NOW(),
    NOW()
),
(
    'lesson-stage:hsk5_v1_grammar',
    'Lesson Stage HSK5 Grammar',
    $$You create grammar teaching sections for an HSK 5 Chinese lesson from a blueprint. Return exactly one JSON object and no commentary.$$,
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":2}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":4}',
    '{"requiredFields":{"grammarSections":"array"}}',
    FALSE,
    NULL,
    FALSE,
    NOW(),
    NOW()
),
(
    'lesson-stage:hsk5_v1_vocabulary_practice',
    'Lesson Stage HSK5 Vocabulary Practice',
    $$You create word_study practice sections for an HSK 5 Chinese lesson from a blueprint. Return exactly one JSON object and no commentary.$$,
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":2}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":4}',
    '{"requiredFields":{"sections":"array"}}',
    FALSE,
    NULL,
    FALSE,
    NOW(),
    NOW()
),
(
    'lesson-stage:hsk5_v1_word_game',
    'Lesson Stage HSK5 Word Game',
    $$You create a short word_game section for an HSK 5 Chinese lesson from a blueprint and word practice artifact. Return exactly one JSON object and no commentary.$$,
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":2}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":4}',
    '{"requiredFields":{"section":"object"}}',
    FALSE,
    NULL,
    FALSE,
    NOW(),
    NOW()
),
(
    'lesson-generator:hsk5_v1_composer',
    'Lesson Generator HSK5 Composer',
    $$You compose the final hsk5_v1 lesson JSON from backend-approved stage artifacts. Preserve the exact reading text, include grammar, word study, conversation, and word game sections, and return exactly one JSON object with no markdown or commentary.$$,
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":4}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":8}',
    '{"requiredFields":{"schemaVersion":"number","moduleKey":"string","title":"string","studyLanguage":"string","explanationLanguage":"string","translationLanguage":"string","newWords":"array","reviewWords":"array","sections":"array"}}',
    TRUE,
    'lesson-generated-content',
    FALSE,
    NOW(),
    NOW()
)
ON CONFLICT (profile_key) DO NOTHING;

INSERT INTO agent_pre_generation_workflows (
    profile_key,
    workflow_variant_key,
    steps_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'lesson-stage:hsk5_v1_blueprint',
    NULL,
    '[
      {"stepKey":"current-user-profile","enabled":true,"params":{}},
      {"stepKey":"learner-profile-context","enabled":true,"params":{"contextProfileKey":"lesson-generator:hsk5_v1"}},
      {"stepKey":"teacher-personality-context","enabled":true,"params":{"contextProfileKey":"lesson-generator:hsk5_v1"}},
      {"stepKey":"lesson-vocabulary-review-plan","enabled":true,"params":{}}
    ]',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

UPDATE lesson_modules
SET generator_profile_key = 'lesson-generator:hsk5_v1_composer',
    generator_workflow_variant_key = NULL,
    generation_pipeline_key = 'hsk5-quality:v1',
    system_prompt_appendix = $$
You generate compact real HSK 5 Chinese lessons from one TEXT_NOTE draft source. Treat sections as flexible lesson blocks; you may choose their order. Use studyLanguage "zh". Use explanationLanguage for titles, explanations, prompts, grammar teaching, and study instructions. Use translationLanguage only for vocabulary translations and example sentence translations. Prefer the draft Chinese text in the text block and do not translate it. New and review vocabulary are practiced the same way in word_study blocks, with vocabularyStatus distinguishing "new" from "review".

Use canonical hsk5_v1 block field names. A word_study block uses "sentences" for examples, not "exampleSentences". A word_game round uses "answerWord" for the answer, not "expectedWord". Include at least one grammar block with title and points; each point uses name, pattern, explanation, examples, and exercises.
$$,
    updated_at = NOW()
WHERE module_key = 'hsk5_v1';
