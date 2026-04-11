package ru.chinesewithai.backend.storedfile.infrastructure.storage;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.storedfile.application.exception.StorageIOException;
import ru.chinesewithai.backend.storedfile.application.port.out.StoredFileStoragePort;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFile;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;
import ru.chinesewithai.backend.storedfile.infrastructure.persistence.SpringDataStoredFileJpaRepository;
import ru.chinesewithai.backend.storedfile.infrastructure.persistence.StoredFileJpaEntity;
import ru.chinesewithai.backend.storedfile.infrastructure.persistence.StoredFileJpaMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Maps each {@link StoredFileId} to a stable object key prefix. Other layers only ever see the UUID
 * primary key; this class owns the key layout and translation.
 */
@Component
@ConditionalOnBean(S3Client.class)
class S3StoredFileStorageAdapter implements StoredFileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3StoredFileStorageAdapter.class);

    private final S3Client s3Client;
    private final SpringDataStoredFileJpaRepository jpa;
    private final String bucket;

    S3StoredFileStorageAdapter(S3Client s3Client, SpringDataStoredFileJpaRepository jpa, StoredFileS3Properties properties) {
        this.s3Client = s3Client;
        this.jpa = jpa;
        this.bucket = properties.bucket();
    }

    /**
     * Versioned prefix keeps room for future key layout migrations without breaking existing rows.
     */
    static String objectKey(StoredFileId id) {
        return "files/v1/" + id.value();
    }

    @Override
    public void putBlob(
            StoredFileId id, long contentLength, String contentType, String originalFileName, InputStream body) {
        var key = objectKey(id);
        var ct = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(ct)
                            .build(),
                    RequestBody.fromInputStream(body, contentLength));
        } catch (S3Exception e) {
            log.error("putObject failed for key {}: {}", key, e.getMessage());
            throw new StorageIOException("Failed to upload object to storage", e);
        }
    }

    @Override
    @Transactional
    public void saveMetadata(
            StoredFileId id, long sizeBytes, String contentType, String originalFileName, Instant createdAt) {
        var entity = new StoredFileJpaEntity(
                id.value(),
                objectKey(id),
                sizeBytes,
                blankToNull(contentType),
                blankToNull(originalFileName),
                createdAt);
        jpa.save(entity);
    }

    @Override
    public void deleteOrphanBlob(StoredFileId id) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey(id))
                    .build());
        } catch (S3Exception e) {
            throw new StorageIOException("Failed to delete orphan object from storage", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findMetadata(StoredFileId id) {
        return jpa.findById(id.value()).map(StoredFileJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StorageGetResult> openGet(StoredFileId id) {
        var entityOpt = jpa.findById(id.value());
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }
        var entity = entityOpt.get();
        var key = entity.getStorageObjectKey();
        try {
            var response = s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            var stream = response;
            String ct = entity.getContentType() != null ? entity.getContentType() : "application/octet-stream";
            String fn = entity.getOriginalFileName() != null ? entity.getOriginalFileName() : "";
            Runnable closer = () -> {
                try {
                    stream.close();
                } catch (Exception ignored) {
                    // closing ResponseInputStream
                }
            };
            return Optional.of(new StorageGetResult(
                    response, entity.getSizeBytes(), ct, fn, closer));
        } catch (S3Exception e) {
            throw new StorageIOException("Failed to open object from storage", e);
        }
    }

    @Override
    @Transactional
    public boolean deleteIfExists(StoredFileId id) {
        var entityOpt = jpa.findById(id.value());
        if (entityOpt.isEmpty()) {
            return false;
        }
        var entity = entityOpt.get();
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(entity.getStorageObjectKey())
                    .build());
        } catch (S3Exception e) {
            throw new StorageIOException("Failed to delete object from storage", e);
        }
        jpa.delete(entity);
        return true;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
    }
}
