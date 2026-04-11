package ru.chinesewithai.backend.storedfile.infrastructure.progress;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.chinesewithai.backend.storedfile.application.port.out.UploadProgressSseRegistry;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;

/**
 * SSE transport lives in infrastructure so the application layer never references Spring MVC event
 * types; {@link UploadProgressSseRegistry} is the corresponding outbound port.
 */
@RestController
@RequestMapping("/api/v1/stored-files")
public class StoredFileSseController {

    private final UploadProgressSseRegistry sseRegistry;

    public StoredFileSseController(UploadProgressSseRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    /**
     * Subscribe before POSTing upload body so progress events are not missed. Disconnecting only
     * detaches the emitter — it does not cancel the upload request handled by {@link
     * ru.chinesewithai.backend.storedfile.api.StoredFileController}.
     */
    @GetMapping(value = "/upload-sessions/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadSessionEvents(@PathVariable UUID sessionId) {
        var emitter = new SseEmitter(0L);
        var session = new FileUploadSessionId(sessionId);
        var connection = new SseEmitterProgressConnection(emitter);
        sseRegistry.attach(session, connection);
        Runnable cleanup = () -> sseRegistry.detach(session);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        return emitter;
    }
}
