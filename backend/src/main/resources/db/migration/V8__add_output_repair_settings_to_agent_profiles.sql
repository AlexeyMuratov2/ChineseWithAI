ALTER TABLE agent_profiles
    ADD COLUMN auto_repair_invalid_output_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE agent_profiles
    ADD COLUMN output_validation_strategy_key VARCHAR(120);

UPDATE agent_profiles
SET auto_repair_invalid_output_enabled = TRUE,
    output_validation_strategy_key = 'lesson-generated-content'
WHERE profile_key = 'lesson-generator:v1';

ALTER TABLE agent_steps
    DROP CONSTRAINT chk_agent_steps_step_type;

ALTER TABLE agent_steps
    ADD CONSTRAINT chk_agent_steps_step_type
        CHECK (step_type IN (
            'SESSION_CREATED',
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
