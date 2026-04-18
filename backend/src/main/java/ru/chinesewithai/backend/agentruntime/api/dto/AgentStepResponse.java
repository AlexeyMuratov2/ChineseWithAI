package ru.chinesewithai.backend.agentruntime.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AgentStepResponse(UUID id, int stepIndex, String type, JsonNode payload, Instant createdAt) {}
