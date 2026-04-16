package ru.chinesewithai.backend.agentruntime.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSessionStatus;

public record AgentSessionResponse(
        UUID sessionId,
        UUID ownerId,
        String profileKey,
        String modelKey,
        String task,
        AgentSessionStatus status,
        JsonNode input,
        JsonNode finalOutput,
        String failureReason,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant updatedAt,
        List<AgentStepResponse> steps) {}
