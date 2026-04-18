CREATE TABLE lesson_vocabulary_items (
    id BIGSERIAL PRIMARY KEY,
    lesson_id UUID NOT NULL,
    user_id UUID NOT NULL,
    hanzi VARCHAR(255) NOT NULL,
    pinyin VARCHAR(255) NOT NULL,
    translation TEXT NOT NULL,
    translation_language VARCHAR(35) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lesson_vocabulary_items_lesson_id
        FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_vocabulary_items_user_id
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_lesson_vocabulary_items_hanzi_not_blank
        CHECK (btrim(hanzi) <> ''),
    CONSTRAINT chk_lesson_vocabulary_items_pinyin_not_blank
        CHECK (btrim(pinyin) <> ''),
    CONSTRAINT chk_lesson_vocabulary_items_translation_not_blank
        CHECK (btrim(translation) <> ''),
    CONSTRAINT chk_lesson_vocabulary_items_translation_language_not_blank
        CHECK (btrim(translation_language) <> '')
);

CREATE UNIQUE INDEX uq_lesson_vocabulary_items_lesson_word
    ON lesson_vocabulary_items (lesson_id, hanzi, pinyin, translation_language);

CREATE INDEX idx_lesson_vocabulary_items_user_created_at
    ON lesson_vocabulary_items (user_id, created_at DESC);

CREATE TABLE learner_vocabulary_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    hanzi VARCHAR(255) NOT NULL,
    pinyin VARCHAR(255) NOT NULL,
    translation TEXT NOT NULL,
    translation_language VARCHAR(35) NOT NULL,
    status VARCHAR(20) NOT NULL,
    mastery_score DOUBLE PRECISION NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_reviewed_at TIMESTAMPTZ NULL,
    review_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_learner_vocabulary_progress_user_id
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_learner_vocabulary_progress_hanzi_not_blank
        CHECK (btrim(hanzi) <> ''),
    CONSTRAINT chk_learner_vocabulary_progress_pinyin_not_blank
        CHECK (btrim(pinyin) <> ''),
    CONSTRAINT chk_learner_vocabulary_progress_translation_not_blank
        CHECK (btrim(translation) <> ''),
    CONSTRAINT chk_learner_vocabulary_progress_translation_language_not_blank
        CHECK (btrim(translation_language) <> ''),
    CONSTRAINT chk_learner_vocabulary_progress_status
        CHECK (status IN ('NEW', 'LEARNING', 'REVIEW', 'MASTERED', 'SUSPENDED')),
    CONSTRAINT chk_learner_vocabulary_progress_mastery_score_range
        CHECK (mastery_score IS NULL OR (mastery_score >= 0 AND mastery_score <= 1)),
    CONSTRAINT chk_learner_vocabulary_progress_review_count_non_negative
        CHECK (review_count >= 0)
);

CREATE UNIQUE INDEX uq_learner_vocabulary_progress_user_word
    ON learner_vocabulary_progress (user_id, hanzi, pinyin, translation_language);

CREATE INDEX idx_learner_vocabulary_progress_review_lookup
    ON learner_vocabulary_progress (user_id, translation_language, status);

INSERT INTO agent_pre_generation_workflows (
    profile_key,
    workflow_variant_key,
    steps_json,
    is_active,
    created_at,
    updated_at
) VALUES (
    'lesson-generator:v1',
    'draft-generation-with-review:v1',
    '[
      {"stepKey":"lesson-vocabulary-review-plan","enabled":true,"params":{}}
    ]',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
