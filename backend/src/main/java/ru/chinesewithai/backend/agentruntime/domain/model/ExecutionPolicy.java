package ru.chinesewithai.backend.agentruntime.domain.model;

public record ExecutionPolicy(int maxSteps) {

    public ExecutionPolicy {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
    }
}
