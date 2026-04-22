CREATE TABLE learner_profile_contexts (
    profile_key VARCHAR(120) PRIMARY KEY,
    content_json JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_learner_profile_contexts_profile_key
        FOREIGN KEY (profile_key) REFERENCES agent_profiles (profile_key) ON DELETE CASCADE,
    CONSTRAINT chk_learner_profile_contexts_profile_key_not_blank
        CHECK (btrim(profile_key) <> '')
);

CREATE INDEX idx_learner_profile_contexts_active
    ON learner_profile_contexts (profile_key, is_active);

INSERT INTO learner_profile_contexts (
    profile_key,
    content_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'lesson-generator:hsk5_v1',
    '{"summary":"Ученик уровня HSK5上, хочет изучать более разговорные конструкции, готовится к экзамену."}'::jsonb,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (profile_key) DO NOTHING;

UPDATE agent_pre_generation_workflows
SET steps_json = '[
      {"stepKey":"current-user-profile","enabled":true,"params":{}},
      {"stepKey":"learner-profile-context","enabled":true,"params":{}},
      {"stepKey":"lesson-vocabulary-review-plan","enabled":true,"params":{}}
    ]',
    updated_at = NOW()
WHERE profile_key = 'lesson-generator:hsk5_v1'
  AND workflow_variant_key IS NULL;
