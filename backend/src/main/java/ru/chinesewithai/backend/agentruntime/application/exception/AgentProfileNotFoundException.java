package ru.chinesewithai.backend.agentruntime.application.exception;

public class AgentProfileNotFoundException extends RuntimeException {
    public AgentProfileNotFoundException(String profileKey) {
        super("Agent profile not found: " + profileKey);
    }
}
