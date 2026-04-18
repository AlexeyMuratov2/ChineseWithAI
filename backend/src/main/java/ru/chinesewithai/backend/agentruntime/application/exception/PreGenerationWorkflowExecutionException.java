package ru.chinesewithai.backend.agentruntime.application.exception;

public class PreGenerationWorkflowExecutionException extends RuntimeException {
    public PreGenerationWorkflowExecutionException(String message) {
        super(message);
    }

    public PreGenerationWorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
