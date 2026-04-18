package ru.chinesewithai.backend.agentruntime.application.exception;

public class AgentWorkflowVariantNotFoundException extends RuntimeException {
    public AgentWorkflowVariantNotFoundException(String profileKey, String workflowVariantKey) {
        super("Pre-generation workflow variant not found for profile %s: %s"
                .formatted(profileKey, workflowVariantKey));
    }
}
