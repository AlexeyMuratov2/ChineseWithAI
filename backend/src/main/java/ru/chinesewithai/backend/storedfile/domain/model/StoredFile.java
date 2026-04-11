package ru.chinesewithai.backend.storedfile.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Technical metadata about a persisted blob. Intentionally free of business meaning (no lesson id,
 * owner id, etc.); those belong to calling modules.
 */
public final class StoredFile {

    private final StoredFileId id;
    private final long sizeBytes;
    private final String contentType;
    private final String originalFileName;
    private final Instant createdAt;

    public StoredFile(
            StoredFileId id, long sizeBytes, String contentType, String originalFileName, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.originalFileName = originalFileName;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public StoredFileId id() {
        return id;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public Optional<String> contentType() {
        return Optional.ofNullable(contentType);
    }

    public Optional<String> originalFileName() {
        return Optional.ofNullable(originalFileName);
    }

    public Instant createdAt() {
        return createdAt;
    }
}
