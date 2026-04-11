package ru.chinesewithai.backend.storedfile.domain.model;

/**
 * Lifecycle of an HTTP-initiated upload. Persisted for observability and for correlating SSE
 * progress with the final {@link StoredFileId}.
 */
public enum UploadSessionState {
    PENDING,
    RECEIVING,
    UPLOADING_TO_STORAGE,
    COMPLETED,
    FAILED
}
