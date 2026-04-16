package ru.chinesewithai.backend.agentruntime.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;

public record AgentStepResponse(UUID id, int stepIndex, AgentStepType type, JsonNode payload, Instant createdAt) {}
