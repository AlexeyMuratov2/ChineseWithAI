ALTER TABLE lesson_draft_sources
    DROP CONSTRAINT IF EXISTS chk_lesson_draft_sources_payload_by_type;

ALTER TABLE lesson_draft_sources
    ADD CONSTRAINT chk_lesson_draft_sources_payload_by_type CHECK (
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
            AND document_file_id IS NOT NULL
            AND (
                text_content IS NULL
                OR btrim(text_content) <> ''
            )
        )
    );
