package ru.chinesewithai.backend.storedfile.infrastructure.progress;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.chinesewithai.backend.storedfile.application.port.out.ProgressSseConnection;

final class SseEmitterProgressConnection implements ProgressSseConnection {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterProgressConnection.class);

    private final SseEmitter emitter;

    SseEmitterProgressConnection(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void sendJsonPayload(String json) {
        try {
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            log.debug("SSE send failed: {}", e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // emitter may already be complete
            }
        }
    }

    @Override
    public void complete() {
        emitter.complete();
    }
}
