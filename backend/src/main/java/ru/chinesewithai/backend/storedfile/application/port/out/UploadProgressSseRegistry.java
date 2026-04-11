package ru.chinesewithai.backend.storedfile.application.port.out;

import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;

/**
 * Tracks active SSE subscribers per upload session. {@link UploadProgressNotifier} implementations
 * push JSON events through this registry.
 */
public interface UploadProgressSseRegistry {

    void attach(FileUploadSessionId sessionId, ProgressSseConnection connection);

    void sendJson(FileUploadSessionId sessionId, String jsonPayload);

    void detach(FileUploadSessionId sessionId);

    /** Sends remaining events should call {@link #sendJson} first; then closes the subscriber. */
    void finish(FileUploadSessionId sessionId);
}
