package ru.chinesewithai.backend.agentruntime.application.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentSessionView(
        UUID sessionId,
        UUID ownerId,
        String profileKey,
        String modelKey,
        String task,
        String workflowVariantKey,
        String status,
        String inputJson,
        String finalOutputJson,
        String failureReason,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant updatedAt,
        List<AgentStepView> steps) {}
