ALTER TABLE agent_sessions
    ADD COLUMN workflow_variant_key VARCHAR(120);

ALTER TABLE agent_sessions
    ADD CONSTRAINT chk_agent_sessions_workflow_variant_key_not_blank
        CHECK (workflow_variant_key IS NULL OR btrim(workflow_variant_key) <> '');

ALTER TABLE agent_steps
    DROP CONSTRAINT chk_agent_steps_step_type;

ALTER TABLE agent_steps
    ADD CONSTRAINT chk_agent_steps_step_type
        CHECK (step_type IN (
            'SESSION_CREATED',
            'PRE_GENERATION_STARTED',
            'PRE_GENERATION_STEP',
            'PRE_GENERATION_COMPLETED',
            'CONTEXT_BUILT',
            'MODEL_REQUEST',
            'MODEL_RESPONSE',
            'TOOL_CALL',
            'TOOL_RESULT',
            'OUTPUT_VALIDATION_FAILED',
            'FINAL_OUTPUT',
            'SESSION_COMPLETED',
            'SESSION_FAILED'
        ));

CREATE TABLE agent_pre_generation_workflows (
    id BIGSERIAL PRIMARY KEY,
    profile_key VARCHAR(120) NOT NULL,
    workflow_variant_key VARCHAR(120) NULL,
    steps_json TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_agent_pre_generation_workflows_profile_key
        FOREIGN KEY (profile_key) REFERENCES agent_profiles (profile_key) ON DELETE CASCADE,
    CONSTRAINT chk_agent_pre_generation_workflows_profile_key_not_blank
        CHECK (btrim(profile_key) <> ''),
    CONSTRAINT chk_agent_pre_generation_workflows_variant_key_not_blank
        CHECK (workflow_variant_key IS NULL OR btrim(workflow_variant_key) <> '')
);

CREATE UNIQUE INDEX uq_agent_pre_generation_workflows_profile_variant
    ON agent_pre_generation_workflows (profile_key, workflow_variant_key)
    WHERE workflow_variant_key IS NOT NULL;

CREATE UNIQUE INDEX uq_agent_pre_generation_workflows_profile_default
    ON agent_pre_generation_workflows (profile_key)
    WHERE workflow_variant_key IS NULL;

CREATE INDEX idx_agent_pre_generation_workflows_profile_active
    ON agent_pre_generation_workflows (profile_key, is_active);

INSERT INTO agent_pre_generation_workflows (
    profile_key,
    workflow_variant_key,
    steps_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'test-agent:v1',
    'personalized-smoke',
    '[
      {"stepKey":"current-user-profile","enabled":true,"params":{}},
      {"stepKey":"input-json-field","enabled":true,"params":{"field":"learnerLevel","target":"system","artifactKey":"learnerLevel","title":"Learner level"}}
    ]',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
