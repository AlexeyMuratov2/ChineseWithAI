package ru.chinesewithai.backend.agentruntime.application.exception;

public class AgentModelNotFoundException extends RuntimeException {
    public AgentModelNotFoundException(String modelKey) {
        super("Agent model not found: " + modelKey);
    }
}
