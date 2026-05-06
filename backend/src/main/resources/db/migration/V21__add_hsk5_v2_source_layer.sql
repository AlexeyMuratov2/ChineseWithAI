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
    'lesson-stage:hsk5_v2_source_normalizer',
    'Lesson Stage HSK5 v2 Source Normalizer',
    $$You normalize ordered textbook sources for an HSK 5 lesson. Return exactly one sourcePack JSON object and no commentary. Preserve source order, transcribe visible Chinese text when images are attached, and never return raw file bytes or base64.$$,
    'qwen3.6-plus',
    'lesson-source-multimodal',
    '[]',
    '{"maxSteps":2}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":4}',
    '{"requiredFields":{"sourcePackVersion":"number","sources":"array","combinedText":"string","sourceRefs":"array"}}',
    TRUE,
    NULL,
    FALSE,
    NOW(),
    NOW()
),
(
    'lesson-generator:hsk5_v2_composer',
    'Lesson Generator HSK5 v2 Composer',
    $$You compose the hsk5_v2 final lesson JSON from a normalized sourcePack. This profile currently validates the source-layer contract only. Return exactly one JSON object and no commentary.$$,
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":4}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":8}',
    '{"requiredFields":{"schemaVersion":"number","moduleKey":"string","title":"string","studyLanguage":"string","explanationLanguage":"string","translationLanguage":"string","newWords":"array","reviewWords":"array","sections":"array","sourcePack":"object"}}',
    TRUE,
    'lesson-generated-content',
    FALSE,
    NOW(),
    NOW()
)
ON CONFLICT (profile_key) DO NOTHING;

INSERT INTO learner_profile_contexts (
    profile_key,
    content_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'lesson-generator:hsk5_v2_composer',
    '{"summary":"Learner is preparing for HSK5 and needs lessons adapted to their current profile; source text must stay faithful to the textbook."}'::jsonb,
    TRUE,
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
    generation_pipeline_key,
    created_at,
    updated_at
) VALUES (
    'hsk5_v2',
    'HSK 5 v2 Source Layer',
    $$This module is the HSK 5 v2 source-layer foundation. It accepts multiple ordered sources, normalizes them into sourcePack first, and composes only from sourcePack. Do not include raw file bytes or contentBase64 in any final lesson JSON.$$,
    1,
    TRUE,
    'lesson-generator:hsk5_v2_composer',
    NULL,
    'hsk5-source-normalized:v2',
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
    'lesson-generator:hsk5_v2_composer',
    NULL,
    '[
      {"params": {}, "enabled": true, "stepKey": "learner-profile-context"},
      {"params": {}, "enabled": true, "stepKey": "lesson-vocabulary-review-plan"}
    ]',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
