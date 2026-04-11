CREATE TABLE lesson_drafts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    user_instructions TEXT NULL,
    explanation_language VARCHAR(35) NOT NULL DEFAULT 'zh',
    translation_language VARCHAR(35) NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lesson_drafts_owner_id
        FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_lesson_drafts_title_not_blank
        CHECK (btrim(title) <> ''),
    CONSTRAINT chk_lesson_drafts_explanation_language_not_blank
        CHECK (btrim(explanation_language) <> ''),
    CONSTRAINT chk_lesson_drafts_translation_language_not_blank
        CHECK (btrim(translation_language) <> '')
);

CREATE INDEX idx_lesson_drafts_owner_updated_at ON lesson_drafts (owner_id, updated_at DESC);

CREATE TABLE lesson_draft_sources (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    position INT NOT NULL,
    text_content TEXT NULL,
    document_file_id UUID NULL,
    document_original_file_name VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lesson_draft_sources_draft_id
        FOREIGN KEY (draft_id) REFERENCES lesson_drafts (id) ON DELETE CASCADE,
    CONSTRAINT chk_lesson_draft_sources_source_type
        CHECK (source_type IN ('TEXT_NOTE', 'DOCUMENT_FILE')),
    CONSTRAINT chk_lesson_draft_sources_position_non_negative
        CHECK (position >= 0),
    CONSTRAINT uq_lesson_draft_sources_draft_position
        UNIQUE (draft_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_lesson_draft_sources_payload_by_type CHECK (
        (
            source_type = 'TEXT_NOTE'
            AND text_content IS NOT NULL
            AND btrim(text_content) <> ''
            AND document_file_id IS NULL
            AND document_original_file_name IS NULL
        )
        OR
        (
            source_type = 'DOCUMENT_FILE'
            AND text_content IS NULL
            AND document_file_id IS NOT NULL
        )
    )
);
CREATE INDEX idx_lesson_draft_sources_draft_position ON lesson_draft_sources (draft_id, position);
