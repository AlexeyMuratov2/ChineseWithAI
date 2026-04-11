-- Technical metadata for user-uploaded blobs. Business modules reference stored_files.id only.
CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    storage_object_key VARCHAR(512) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content_type VARCHAR(255) NULL,
    original_file_name VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_stored_files_size_bytes_positive CHECK (size_bytes > 0),
    CONSTRAINT uq_stored_files_storage_object_key UNIQUE (storage_object_key)
);

CREATE INDEX idx_stored_files_created_at ON stored_files (created_at DESC);

-- Upload sessions: progress tracking and binding to final stored_files row after success.
CREATE TABLE file_upload_sessions (
    id UUID PRIMARY KEY,
    state VARCHAR(40) NOT NULL,
    upload_scenario VARCHAR(64) NOT NULL,
    bytes_received BIGINT NOT NULL DEFAULT 0,
    bytes_expected BIGINT NULL,
    percent INT NULL,
    declared_content_type VARCHAR(255) NULL,
    original_file_name VARCHAR(255) NULL,
    error_message TEXT NULL,
    result_file_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_file_upload_sessions_result_file
        FOREIGN KEY (result_file_id) REFERENCES stored_files (id) ON DELETE SET NULL,
    CONSTRAINT chk_file_upload_sessions_state
        CHECK (state IN (
            'PENDING',
            'RECEIVING',
            'UPLOADING_TO_STORAGE',
            'COMPLETED',
            'FAILED'
        )),
    CONSTRAINT chk_file_upload_sessions_bytes_received_non_negative
        CHECK (bytes_received >= 0),
    CONSTRAINT chk_file_upload_sessions_percent_range
        CHECK (percent IS NULL OR (percent >= 0 AND percent <= 100))
);

CREATE INDEX idx_file_upload_sessions_state ON file_upload_sessions (state);
