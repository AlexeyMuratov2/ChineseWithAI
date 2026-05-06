package ru.chinesewithai.backend.storedfile.application.api;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import ru.chinesewithai.backend.storedfile.application.command.CreateUploadSessionCommand;
import ru.chinesewithai.backend.storedfile.application.command.DeleteStoredFileCommand;
import ru.chinesewithai.backend.storedfile.application.command.StoreFileCommand;
import ru.chinesewithai.backend.storedfile.application.port.out.FileUploadSessionSnapshot;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;

/**
 * Stable entry point for other Spring Modulith modules. Inject this interface (not web controllers
 * or S3 adapters) from feature modules.
 */
public interface StoredFileFacade {

    FileUploadSessionId createUploadSession(CreateUploadSessionCommand command);

    Optional<FileUploadSessionSnapshot> getUploadSession(FileUploadSessionId sessionId);

    /**
     * Accepts the raw HTTP body for a previously created session, enforces security strategies,
     * streams bytes to object storage, persists technical metadata, and completes the session.
     *
     * <p>Closing the SSE connection does not cancel this work; the client may still observe final
     * state via {@link #getUploadSession} if polling is added later.
     */
    StoredFileMetadata receiveSessionUpload(
            FileUploadSessionId sessionId,
            long contentLength,
            Optional<String> contentType,
            Optional<String> originalFileName,
            InputStream body);

    StoredFileId store(StoreFileCommand command, InputStream content, ProgressSink progressSink);

    Optional<StoredFileMetadata> getMetadata(StoredFileId id);

    Optional<StoredFileContent> openContent(StoredFileId id);

    default Optional<StoredFileContent> openContent(UUID id) {
        return openContent(StoredFileId.of(id));
    }

    DeleteStoredFileResult delete(DeleteStoredFileCommand command);
}
