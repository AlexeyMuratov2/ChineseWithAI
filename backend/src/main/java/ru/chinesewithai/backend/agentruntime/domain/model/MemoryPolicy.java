package ru.chinesewithai.backend.agentruntime.domain.model;

public record MemoryPolicy(boolean includePreviousSteps, int maxStepHistoryEntries) {

    public MemoryPolicy {
        if (maxStepHistoryEntries < 0) {
            throw new IllegalArgumentException("maxStepHistoryEntries must be non-negative");
        }
    }
}
