package ru.chinesewithai.backend.storedfile.application.service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.storedfile.application.api.DeleteStoredFileResult;
import ru.chinesewithai.backend.storedfile.application.api.ProgressSink;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileContent;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileMetadata;
import ru.chinesewithai.backend.storedfile.application.command.CreateUploadSessionCommand;
import ru.chinesewithai.backend.storedfile.application.command.DeleteStoredFileCommand;
import ru.chinesewithai.backend.storedfile.application.command.StoreFileCommand;
import ru.chinesewithai.backend.storedfile.application.exception.FileUploadRejectedException;
import ru.chinesewithai.backend.storedfile.application.exception.InvalidUploadSessionStateException;
import ru.chinesewithai.backend.storedfile.application.exception.StorageIOException;
import ru.chinesewithai.backend.storedfile.application.exception.UploadSessionNotFoundException;
import ru.chinesewithai.backend.storedfile.application.io.CountingProgressInputStream;
import ru.chinesewithai.backend.storedfile.application.port.out.FileUploadSessionRepository;
import ru.chinesewithai.backend.storedfile.application.port.out.FileUploadSessionSnapshot;
import ru.chinesewithai.backend.storedfile.application.port.out.StoredFileStoragePort;
import ru.chinesewithai.backend.storedfile.application.port.out.UploadProgressNotifier;
import ru.chinesewithai.backend.storedfile.application.security.FileUploadSecurityStrategyFactory;
import ru.chinesewithai.backend.storedfile.application.security.UploadSecurityContext;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;
import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

/**
 * Orchestrates upload sessions, security strategy hooks, storage writes, and metadata persistence.
 * Transaction boundaries intentionally exclude long-running object-storage I/O — session rows are
 * updated in short repository calls while the input stream is consumed.
 */
@Service
public class StoredFileApplicationService implements StoredFileFacade {

    private static final long PROGRESS_STRIDE_BYTES = 64 * 1024;

    private final FileUploadSessionRepository uploadSessions;
    private final StoredFileStoragePort storage;
    private final FileUploadSecurityStrategyFactory securityFactory;
    private final UploadProgressNotifier progressNotifier;

    public StoredFileApplicationService(
            FileUploadSessionRepository uploadSessions,
            StoredFileStoragePort storage,
            FileUploadSecurityStrategyFactory securityFactory,
            UploadProgressNotifier progressNotifier) {
        this.uploadSessions = uploadSessions;
        this.storage = storage;
        this.securityFactory = securityFactory;
        this.progressNotifier = progressNotifier;
    }

    @Override
    @Transactional
    public FileUploadSessionId createUploadSession(CreateUploadSessionCommand command) {
        var id = FileUploadSessionId.random();
        var now = Instant.now();
        uploadSessions.insert(
                id,
                command.scenario(),
                command.expectedContentLength(),
                command.declaredContentType(),
                command.originalFileName(),
                now);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileUploadSessionSnapshot> getUploadSession(FileUploadSessionId sessionId) {
        return uploadSessions.find(sessionId);
    }

    @Override
    public StoredFileMetadata receiveSessionUpload(
            FileUploadSessionId sessionId,
            long contentLength,
            Optional<String> contentType,
            Optional<String> originalFileName,
            Optional<UUID> principalId,
            InputStream body) {
        var snapshot =
                uploadSessions.find(sessionId).orElseThrow(() -> new UploadSessionNotFoundException(sessionId.value()));

        if (snapshot.state() != UploadSessionState.PENDING) {
            throw InvalidUploadSessionStateException.notReceiving(snapshot.state());
        }

        var scenario = snapshot.uploadScenario();
        var strategy = securityFactory.forScenario(scenario);
        var resolvedContentType = contentType.or(() -> snapshot.declaredContentType()).orElse(null);
        var resolvedFileName = originalFileName.or(() -> snapshot.originalFileName()).orElse(null);

        var ctx = UploadSecurityContext.httpUpload(
                scenario,
                resolvedFileName,
                resolvedContentType,
                contentLength,
                principalId.orElse(null));

        var now = Instant.now();
        uploadSessions.updateState(sessionId, UploadSessionState.RECEIVING, now);

        var fileId = StoredFileId.random();
        var expectedTotal = snapshot.bytesExpected().or(() -> Optional.of(contentLength));

        var counting = new CountingProgressInputStream(
                body,
                expectedTotal,
                resolveStride(contentLength),
                (bytesRead, percent) -> {
                    var ts = Instant.now();
                    uploadSessions.updateProgress(sessionId, bytesRead, percent, ts);
                    progressNotifier.progress(
                            sessionId,
                            UploadSessionState.RECEIVING,
                            bytesRead,
                            expectedTotal,
                            percent);
                });

        boolean blobWritten = false;
        try {
            strategy.validateBeforeStream(ctx);
            storage.putBlob(
                    fileId,
                    contentLength,
                    resolvedContentType,
                    resolvedFileName,
                    counting);
            blobWritten = true;
            counting.finishProgress();

            strategy.validateAfterStream(ctx, counting.getTotalBytesRead());

            var uploadingAt = Instant.now();
            uploadSessions.updateState(sessionId, UploadSessionState.UPLOADING_TO_STORAGE, uploadingAt);
            progressNotifier.progress(
                    sessionId,
                    UploadSessionState.UPLOADING_TO_STORAGE,
                    counting.getTotalBytesRead(),
                    expectedTotal,
                    Optional.of(100));

            var createdAt = Instant.now();
            storage.saveMetadata(
                    fileId,
                    counting.getTotalBytesRead(),
                    resolvedContentType,
                    resolvedFileName,
                    createdAt);

            uploadSessions.complete(sessionId, fileId, Instant.now());
            progressNotifier.completed(sessionId, fileId);

            return StoredFileMetadata.from(storage.findMetadata(fileId).orElseThrow());
        } catch (FileUploadRejectedException e) {
            markSessionFailed(sessionId, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            if (blobWritten) {
                try {
                    storage.deleteOrphanBlob(fileId);
                } catch (RuntimeException cleanup) {
                    e.addSuppressed(cleanup);
                }
            }
            markSessionFailed(sessionId, e.getMessage());
            throw e;
        }
    }

    @Override
    public StoredFileId store(StoreFileCommand command, InputStream content, ProgressSink progressSink) {
        var strategy = securityFactory.forScenario(command.scenario());
        var ctx = UploadSecurityContext.programmatic(command.scenario());
        var fileId = StoredFileId.random();
        var expected = Optional.of(command.contentLength());
        var counting = new CountingProgressInputStream(
                content,
                expected,
                resolveStride(command.contentLength()),
                (bytesRead, percent) ->
                        progressSink.onProgress(percent.orElse(0), bytesRead, expected.orElse(null)));

        boolean blobWritten = false;
        try {
            strategy.validateBeforeStream(ctx);
            storage.putBlob(
                    fileId,
                    command.contentLength(),
                    command.contentType().orElse(null),
                    command.originalFileName().orElse(null),
                    counting);
            blobWritten = true;
            counting.finishProgress();
            strategy.validateAfterStream(ctx, counting.getTotalBytesRead());
            storage.saveMetadata(
                    fileId,
                    counting.getTotalBytesRead(),
                    command.contentType().orElse(null),
                    command.originalFileName().orElse(null),
                    Instant.now());
            return fileId;
        } catch (RuntimeException e) {
            if (blobWritten) {
                try {
                    storage.deleteOrphanBlob(fileId);
                } catch (RuntimeException cleanup) {
                    e.addSuppressed(cleanup);
                }
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileMetadata> getMetadata(StoredFileId id) {
        return storage.findMetadata(id).map(StoredFileMetadata::from);
    }

    @Override
    public Optional<StoredFileContent> openContent(StoredFileId id) {
        return storage
                .openGet(id)
                .map(r -> new StoredFileContent(
                        r.stream(),
                        r.sizeBytes(),
                        Optional.ofNullable(r.contentType()),
                        Optional.ofNullable(r.originalFileName()).filter(s -> !s.isBlank()),
                        r.closeAction()));
    }

    @Override
    public DeleteStoredFileResult delete(DeleteStoredFileCommand command) {
        var id = StoredFileId.of(command.fileId());
        try {
            boolean existed = storage.deleteIfExists(id);
            return existed ? DeleteStoredFileResult.SUCCESS : DeleteStoredFileResult.ALREADY_ABSENT;
        } catch (StorageIOException e) {
            return DeleteStoredFileResult.STORAGE_FAILURE;
        }
    }

    private void markSessionFailed(FileUploadSessionId sessionId, String message) {
        try {
            uploadSessions.fail(sessionId, message == null ? "Unknown error" : message, Instant.now());
            progressNotifier.failed(sessionId, message == null ? "Unknown error" : message);
        } catch (RuntimeException ignored) {
            // best-effort notification after primary failure
        }
    }

    private static long resolveStride(long contentLength) {
        if (contentLength <= 0) {
            return PROGRESS_STRIDE_BYTES;
        }
        return Math.min(PROGRESS_STRIDE_BYTES, Math.max(1, contentLength / 100));
    }
}
