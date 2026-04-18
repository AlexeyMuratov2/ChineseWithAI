package ru.chinesewithai.backend.agentruntime.application.port.out;

public interface PreGenerationStep {
    String key();

    PreGenerationStepResult execute(PreGenerationStepRequest request);
}
