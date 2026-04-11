package ru.chinesewithai.backend.storedfile.infrastructure.progress;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.storedfile.application.port.out.ProgressSseConnection;
import ru.chinesewithai.backend.storedfile.application.port.out.UploadProgressSseRegistry;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;

@Component
public class InMemoryUploadProgressSseRegistry implements UploadProgressSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(InMemoryUploadProgressSseRegistry.class);

    private final Map<UUID, ProgressSseConnection> connections = new ConcurrentHashMap<>();

    @Override
    public void attach(FileUploadSessionId sessionId, ProgressSseConnection connection) {
        var id = sessionId.value();
        connections.put(id, connection);
    }

    @Override
    public void sendJson(FileUploadSessionId sessionId, String jsonPayload) {
        var c = connections.get(sessionId.value());
        if (c == null) {
            return;
        }
        try {
            c.sendJsonPayload(jsonPayload);
        } catch (RuntimeException e) {
            log.debug("SSE subscriber dropped for {}: {}", sessionId.value(), e.getMessage());
            connections.remove(sessionId.value());
        }
    }

    @Override
    public void detach(FileUploadSessionId sessionId) {
        connections.remove(sessionId.value());
    }

    @Override
    public void finish(FileUploadSessionId sessionId) {
        var c = connections.remove(sessionId.value());
        if (c != null) {
            try {
                c.complete();
            } catch (RuntimeException e) {
                log.debug("SSE complete failed: {}", e.getMessage());
            }
        }
    }
}
