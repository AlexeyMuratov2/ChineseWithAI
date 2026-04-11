package ru.chinesewithai.backend.storedfile.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistence model for {@link ru.chinesewithai.backend.storedfile.domain.model.StoredFile}.
 *
 * <p>{@code storageObjectKey} is an infrastructure concern: it must never appear in HTTP DTOs or
 * in the public {@code storedfile.application.api} facade returned to other modules.
 */
@Entity
@Table(name = "stored_files")
public class StoredFileJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "storage_object_key", nullable = false, length = 512, unique = true)
    private String storageObjectKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StoredFileJpaEntity() {}

    public StoredFileJpaEntity(
            UUID id,
            String storageObjectKey,
            long sizeBytes,
            String contentType,
            String originalFileName,
            Instant createdAt) {
        this.id = id;
        this.storageObjectKey = storageObjectKey;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.originalFileName = originalFileName;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getStorageObjectKey() {
        return storageObjectKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
