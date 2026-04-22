CREATE TABLE teacher_personality_contexts (
    profile_key VARCHAR(120) PRIMARY KEY,
    content_json JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_teacher_personality_contexts_profile_key
        FOREIGN KEY (profile_key) REFERENCES agent_profiles (profile_key) ON DELETE CASCADE,
    CONSTRAINT chk_teacher_personality_contexts_profile_key_not_blank
        CHECK (btrim(profile_key) <> '')
);

CREATE INDEX idx_teacher_personality_contexts_active
    ON teacher_personality_contexts (profile_key, is_active);

INSERT INTO teacher_personality_contexts (
    profile_key,
    content_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'lesson-generator:hsk5_v1',
    '{"teacherCharacter":"\u0422\u044b \u0432\u0435\u0441\u0435\u043b\u044b\u0439 \u0443\u0447\u0438\u0442\u0435\u043b\u044c, \u043a\u043e\u0442\u043e\u0440\u044b\u0439 \u043c\u043e\u0436\u0435\u0442 \u043f\u043e\u0434\u0431\u043e\u0434\u0440\u0438\u0442\u044c \u0438 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0430\u0442\u044c."}'::jsonb,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (profile_key) DO NOTHING;

UPDATE agent_pre_generation_workflows
SET steps_json = '[
      {"stepKey":"current-user-profile","enabled":true,"params":{}},
      {"stepKey":"learner-profile-context","enabled":true,"params":{}},
      {"stepKey":"teacher-personality-context","enabled":true,"params":{}},
      {"stepKey":"lesson-vocabulary-review-plan","enabled":true,"params":{}}
    ]',
    updated_at = NOW()
WHERE profile_key = 'lesson-generator:hsk5_v1'
  AND workflow_variant_key IS NULL;
