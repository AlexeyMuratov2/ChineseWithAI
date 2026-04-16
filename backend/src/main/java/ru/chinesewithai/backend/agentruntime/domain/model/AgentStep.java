package ru.chinesewithai.backend.agentruntime.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentStep(
        UUID id, UUID sessionId, int stepIndex, AgentStepType type, String payloadJson, Instant createdAt) {

    public AgentStep {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("stepIndex must be non-negative");
        }
        Objects.requireNonNull(type, "type must not be null");
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("payloadJson must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AgentStep create(
            UUID sessionId, int stepIndex, AgentStepType type, String payloadJson, Instant createdAt) {
        return new AgentStep(UUID.randomUUID(), sessionId, stepIndex, type, payloadJson, createdAt);
    }
}
