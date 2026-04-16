package ru.chinesewithai.backend.agentruntime.application.exception;

import java.util.UUID;

public class AgentSessionNotFoundException extends RuntimeException {
    public AgentSessionNotFoundException(UUID sessionId) {
        super("Agent session not found: " + sessionId);
    }
}
