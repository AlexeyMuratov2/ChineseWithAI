package ru.chinesewithai.backend.agentruntime.application.port.out;

public interface PreGenerationStepCatalog {
    boolean contains(String stepKey);

    PreGenerationStep getRequired(String stepKey);
}
