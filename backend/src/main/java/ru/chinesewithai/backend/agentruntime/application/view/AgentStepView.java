package ru.chinesewithai.backend.agentruntime.application.view;

import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;

public record AgentStepView(UUID id, int stepIndex, AgentStepType type, String payloadJson, Instant createdAt) {}
