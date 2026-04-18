package ru.chinesewithai.backend.agentruntime.application.view;

import java.time.Instant;
import java.util.UUID;

public record AgentStepView(UUID id, int stepIndex, String type, String payloadJson, Instant createdAt) {}
