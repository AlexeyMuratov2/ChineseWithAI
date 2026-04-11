package ru.chinesewithai.backend.storedfile.application.port.out;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFile;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;

/**
 * Single outbound port for object bytes + metadata rows. Keeps S3/JPA orchestration and storage key
 * layout inside infrastructure so application code never handles raw keys.
 *
 * <p>Upload is split into {@link #putBlob} then {@link #saveMetadata} so application-layer security
 * can run {@code validateAfterStream} after the body is fully consumed but before the metadata row
 * exists. If validation fails after {@link #putBlob}, call {@link #deleteOrphanBlob} to compensate.
 */
public interface StoredFileStoragePort {

    void putBlob(
            StoredFileId id,
            long contentLength,
            String contentType,
            String originalFileName,
            InputStream body);

    void saveMetadata(StoredFileId id, long sizeBytes, String contentType, String originalFileName, Instant createdAt);

    /** Removes bytes from object storage when no DB row should exist (validation failure after put). */
    void deleteOrphanBlob(StoredFileId id);

    Optional<StoredFile> findMetadata(StoredFileId id);

    Optional<StorageGetResult> openGet(StoredFileId id);

    /**
     * Deletes object storage first (tolerating missing keys), then removes the metadata row. Throws
     * {@link ru.chinesewithai.backend.storedfile.application.exception.StorageIOException} when
     * storage deletion fails with a retryable/non-404 error.
     *
     * @return true if a row existed and was processed, false if there was nothing to delete
     */
    boolean deleteIfExists(StoredFileId id);

    record StorageGetResult(InputStream stream, long sizeBytes, String contentType, String originalFileName, Runnable closeAction) {}
}
