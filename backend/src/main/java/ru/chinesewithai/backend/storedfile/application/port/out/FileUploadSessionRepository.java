package ru.chinesewithai.backend.storedfile.application.port.out;

import java.time.Instant;
import java.util.Optional;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;
import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

public interface FileUploadSessionRepository {

    void insert(
            FileUploadSessionId id,
            UploadScenario scenario,
            Optional<Long> bytesExpected,
            Optional<String> declaredContentType,
            Optional<String> originalFileName,
            Instant now);

    Optional<FileUploadSessionSnapshot> find(FileUploadSessionId id);

    void updateState(FileUploadSessionId id, UploadSessionState state, Instant now);

    void updateProgress(FileUploadSessionId id, long bytesReceived, Optional<Integer> percent, Instant now);

    void complete(FileUploadSessionId id, StoredFileId resultFileId, Instant now);

    void fail(FileUploadSessionId id, String errorMessage, Instant now);
}
