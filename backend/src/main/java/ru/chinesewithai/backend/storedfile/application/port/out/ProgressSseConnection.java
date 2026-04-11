package ru.chinesewithai.backend.storedfile.application.port.out;

/**
 * Minimal server-sent event sink so upload progress can stay framework-agnostic at the application
 * boundary. Implemented in infrastructure by wrapping Spring MVC's SseEmitter.
 */
public interface ProgressSseConnection {

    void sendJsonPayload(String json);

    void complete();
}
