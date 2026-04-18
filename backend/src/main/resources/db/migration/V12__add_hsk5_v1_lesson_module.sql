ALTER TABLE lesson_modules
    ADD COLUMN generator_profile_key VARCHAR(120);

ALTER TABLE lesson_modules
    ADD COLUMN generator_workflow_variant_key VARCHAR(120);

UPDATE lesson_modules
SET generator_profile_key = 'lesson-generator:v1',
    generator_workflow_variant_key = 'draft-generation-with-review:v1'
WHERE generator_profile_key IS NULL;

ALTER TABLE lesson_modules
    ALTER COLUMN generator_profile_key SET NOT NULL;

ALTER TABLE lesson_modules
    ADD CONSTRAINT fk_lesson_modules_generator_profile_key
        FOREIGN KEY (generator_profile_key) REFERENCES agent_profiles (profile_key) ON DELETE RESTRICT;

ALTER TABLE lesson_modules
    ADD CONSTRAINT chk_lesson_modules_generator_profile_key_not_blank
        CHECK (btrim(generator_profile_key) <> '');

ALTER TABLE lesson_modules
    ADD CONSTRAINT chk_lesson_modules_generator_workflow_variant_key_not_blank
        CHECK (generator_workflow_variant_key IS NULL OR btrim(generator_workflow_variant_key) <> '');

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
) VALUES (
    'lesson-generator:hsk5_v1',
    'Lesson Generator HSK5 v1',
    'You are a deterministic HSK 5 Chinese lesson generation agent. The learner level is HSK5. Return exactly one JSON object that matches the output contract and the hsk5_v1 lesson-module appendix. Do not wrap JSON in markdown fences or add commentary.

Spaced repetition: the session context may include a vocabularyReviewPlan with mustReview and shouldReview. Populate top-level reviewWords with {word, pinyin, translation} entries for review vocabulary that you actually practice in the lesson. Put every practiced review word into a word_study block with vocabularyStatus "review". If the plan is empty, set reviewWords to [].

The draft TEXT_NOTE is the source reading text. Keep that text in Chinese in the text block, do not translate it, and keep the lesson lively with conversation prompts and a short word game.',
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

INSERT INTO lesson_modules (
    module_key,
    display_name,
    system_prompt_appendix,
    schema_version,
    is_active,
    generator_profile_key,
    generator_workflow_variant_key,
    created_at,
    updated_at
) VALUES (
    'hsk5_v1',
    'HSK 5 v1',
    'You generate compact real HSK 5 Chinese lessons from one TEXT_NOTE draft source. Treat sections as flexible lesson blocks; you may choose their order. Use studyLanguage "zh". Use explanationLanguage for titles, explanations, prompts, and instructions. Use translationLanguage only for vocabulary translations and word-study sentence translations. The text block must contain the draft Chinese text exactly after trimming and must not include a translation field. New and review vocabulary are practiced the same way in word_study blocks, with vocabularyStatus distinguishing "new" from "review".',
    1,
    TRUE,
    'lesson-generator:hsk5_v1',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (module_key) DO NOTHING;

INSERT INTO agent_pre_generation_workflows (
    profile_key,
    workflow_variant_key,
    steps_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'lesson-generator:hsk5_v1',
    NULL,
    '[
      {"stepKey":"current-user-profile","enabled":true,"params":{}},
      {"stepKey":"lesson-vocabulary-review-plan","enabled":true,"params":{}}
    ]',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
