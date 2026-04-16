CREATE TABLE agent_profiles (
    profile_key VARCHAR(120) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    system_prompt TEXT NOT NULL,
    model_key VARCHAR(120) NOT NULL,
    context_builder_key VARCHAR(120) NOT NULL,
    allowed_tools_json TEXT NOT NULL,
    execution_policy_json TEXT NOT NULL,
    memory_policy_json TEXT NOT NULL,
    output_contract_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_agent_profiles_profile_key_not_blank
        CHECK (btrim(profile_key) <> ''),
    CONSTRAINT chk_agent_profiles_display_name_not_blank
        CHECK (btrim(display_name) <> ''),
    CONSTRAINT chk_agent_profiles_model_key_not_blank
        CHECK (btrim(model_key) <> ''),
    CONSTRAINT chk_agent_profiles_context_builder_key_not_blank
        CHECK (btrim(context_builder_key) <> '')
);

CREATE TABLE agent_sessions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    profile_key VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    input_json TEXT NOT NULL,
    final_output_json TEXT NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ NULL,
    finished_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_agent_sessions_owner_id
        FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_sessions_profile_key
        FOREIGN KEY (profile_key) REFERENCES agent_profiles (profile_key),
    CONSTRAINT chk_agent_sessions_status
        CHECK (status IN ('CREATED', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_agent_sessions_owner_created_at ON agent_sessions (owner_id, created_at DESC);

CREATE TABLE agent_steps (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    step_index INT NOT NULL,
    step_type VARCHAR(40) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_agent_steps_session_id
        FOREIGN KEY (session_id) REFERENCES agent_sessions (id) ON DELETE CASCADE,
    CONSTRAINT chk_agent_steps_step_index_non_negative
        CHECK (step_index >= 0),
    CONSTRAINT chk_agent_steps_step_type
        CHECK (step_type IN (
            'SESSION_CREATED',
            'CONTEXT_BUILT',
            'MODEL_REQUEST',
            'MODEL_RESPONSE',
            'TOOL_CALL',
            'TOOL_RESULT',
            'FINAL_OUTPUT',
            'SESSION_COMPLETED',
            'SESSION_FAILED'
        )),
    CONSTRAINT uq_agent_steps_session_step_index
        UNIQUE (session_id, step_index)
);

CREATE INDEX idx_agent_steps_session_step_index ON agent_steps (session_id, step_index);

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
    created_at,
    updated_at
) VALUES (
    'test-agent:v1',
    'Test Agent v1',
    'You are a deterministic runtime smoke-test agent. Use the available tool before returning the final JSON output.',
    'fake-model',
    'default',
    '["get_static_test_data"]',
    '{"maxSteps":4}',
    '{"includePreviousSteps":true,"maxStepHistoryEntries":8}',
    '{"requiredFields":{"summary":"string","toolMessage":"string"}}',
    NOW(),
    NOW()
);
