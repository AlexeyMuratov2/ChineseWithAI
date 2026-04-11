package ru.chinesewithai.backend.storedfile.application.port.out;

import java.util.Optional;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;
import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

/**
 * Bridges upload orchestration to transport (SSE). Default no-op implementation exists for tests and
 * programmatic uploads without a session subscriber.
 */
public interface UploadProgressNotifier {

    void progress(
            FileUploadSessionId sessionId,
            UploadSessionState state,
            long bytesReceived,
            Optional<Long> bytesExpected,
            Optional<Integer> percent);

    void completed(FileUploadSessionId sessionId, StoredFileId fileId);

    void failed(FileUploadSessionId sessionId, String message);
}
