ALTER TABLE agent_sessions
    ADD COLUMN model_key VARCHAR(120);

ALTER TABLE agent_sessions
    ADD COLUMN task TEXT;

UPDATE agent_sessions session
SET model_key = profile.model_key
FROM agent_profiles profile
WHERE session.profile_key = profile.profile_key
  AND (session.model_key IS NULL OR btrim(session.model_key) = '');

UPDATE agent_sessions
SET task = input_json
WHERE task IS NULL;

ALTER TABLE agent_sessions
    ALTER COLUMN model_key SET NOT NULL;

ALTER TABLE agent_sessions
    ALTER COLUMN task SET NOT NULL;

ALTER TABLE agent_sessions
    ALTER COLUMN input_json DROP NOT NULL;

ALTER TABLE agent_sessions
    ADD CONSTRAINT chk_agent_sessions_model_key_not_blank
        CHECK (btrim(model_key) <> '');

ALTER TABLE agent_sessions
    ADD CONSTRAINT chk_agent_sessions_task_not_blank
        CHECK (btrim(task) <> '');

ALTER TABLE agent_profiles
    ADD COLUMN is_visible BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE agent_profiles
SET is_visible = FALSE
WHERE profile_key = 'test-agent:v1';

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
    'assistant:v1',
    'Assistant v1',
    'You are a helpful general-purpose assistant. Solve the task and return only the final JSON object that matches the required contract.',
    'deepseek-chat',
    'default',
    '[]',
    '{"maxSteps":6}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":8}',
    '{"requiredFields":{"answer":"string"}}',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (profile_key) DO NOTHING;
