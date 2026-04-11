package ru.chinesewithai.backend.storedfile.infrastructure.progress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.storedfile.application.port.out.UploadProgressNotifier;
import ru.chinesewithai.backend.storedfile.application.port.out.UploadProgressSseRegistry;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;
import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

/**
 * Pushes upload lifecycle events to any subscribed SSE client. Safe when no client is connected —
 * events are simply dropped besides DB-backed session state.
 */
@Component
public class SseUploadProgressNotifier implements UploadProgressNotifier {

    private final UploadProgressSseRegistry sseRegistry;
    private final ObjectMapper objectMapper;

    public SseUploadProgressNotifier(UploadProgressSseRegistry sseRegistry, ObjectMapper objectMapper) {
        this.sseRegistry = sseRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void progress(
            FileUploadSessionId sessionId,
            UploadSessionState state,
            long bytesReceived,
            Optional<Long> bytesExpected,
            Optional<Integer> percent) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("type", "PROGRESS");
        payload.put("state", state.name());
        payload.put("bytesReceived", bytesReceived);
        bytesExpected.ifPresent(v -> payload.put("bytesExpected", v));
        percent.ifPresent(v -> payload.put("percent", v));
        sseRegistry.sendJson(sessionId, write(payload));
    }

    @Override
    public void completed(FileUploadSessionId sessionId, StoredFileId fileId) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("type", "COMPLETED");
        payload.put("fileId", fileId.value().toString());
        sseRegistry.sendJson(sessionId, write(payload));
        sseRegistry.finish(sessionId);
    }

    @Override
    public void failed(FileUploadSessionId sessionId, String message) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("type", "FAILED");
        payload.put("message", message);
        sseRegistry.sendJson(sessionId, write(payload));
        sseRegistry.finish(sessionId);
    }

    private String write(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"ERROR\",\"message\":\"serialization failed\"}";
        }
    }
}
