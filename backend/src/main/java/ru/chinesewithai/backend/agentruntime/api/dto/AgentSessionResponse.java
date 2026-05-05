package ru.chinesewithai.backend.agentruntime.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentSessionResponse(
        UUID sessionId,
        String profileKey,
        String modelKey,
        String task,
        String workflowVariantKey,
        String status,
        JsonNode input,
        JsonNode finalOutput,
        String failureReason,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant updatedAt,
        List<AgentStepResponse> steps) {}
