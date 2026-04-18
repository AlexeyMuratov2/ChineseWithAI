package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Optional;

public interface PreGenerationWorkflowRegistry {
    Optional<PreGenerationWorkflow> findVariant(String profileKey, String workflowVariantKey);

    Optional<PreGenerationWorkflow> findDefault(String profileKey);
}
