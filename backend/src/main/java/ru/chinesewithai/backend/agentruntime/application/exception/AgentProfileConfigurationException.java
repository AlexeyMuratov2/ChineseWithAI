package ru.chinesewithai.backend.agentruntime.application.exception;

public class AgentProfileConfigurationException extends RuntimeException {
    public AgentProfileConfigurationException(String message) {
        super(message);
    }

    public AgentProfileConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
