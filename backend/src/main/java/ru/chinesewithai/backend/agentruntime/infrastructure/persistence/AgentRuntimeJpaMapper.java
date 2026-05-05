package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSessionStatus;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;

final class AgentRuntimeJpaMapper {

    private AgentRuntimeJpaMapper() {}

    static AgentSessionJpaEntity toEntity(AgentSession session) {
        return new AgentSessionJpaEntity(
                session.id(),
                session.profileKey(),
                session.modelKey(),
                session.task(),
                session.systemPromptAppendix(),
                session.workflowVariantKey(),
                session.status().name(),
                session.inputJson(),
                session.finalOutputJson(),
                session.failureReason(),
                session.createdAt(),
                session.startedAt(),
                session.finishedAt(),
                session.updatedAt());
    }

    static AgentSession toDomain(AgentSessionJpaEntity entity) {
        return AgentSession.reconstitute(
                entity.getId(),
                entity.getProfileKey(),
                entity.getModelKey(),
                entity.getTask(),
                entity.getSystemPromptAppendix(),
                entity.getWorkflowVariantKey(),
                AgentSessionStatus.valueOf(entity.getStatus()),
                entity.getInputJson(),
                entity.getFinalOutputJson(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getUpdatedAt());
    }

    static AgentStepJpaEntity toEntity(AgentStep step) {
        return new AgentStepJpaEntity(
                step.id(),
                step.sessionId(),
                step.stepIndex(),
                step.type().name(),
                step.payloadJson(),
                step.createdAt());
    }

    static AgentStep toDomain(AgentStepJpaEntity entity) {
        return new AgentStep(
                entity.getId(),
                entity.getSessionId(),
                entity.getStepIndex(),
                AgentStepType.valueOf(entity.getStepType()),
                entity.getPayloadJson(),
                entity.getCreatedAt());
    }
}
