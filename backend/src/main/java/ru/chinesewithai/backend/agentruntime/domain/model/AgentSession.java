package ru.chinesewithai.backend.agentruntime.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AgentSession {

    private final UUID id;
    private final String profileKey;
    private final String modelKey;
    private final String task;
    private final String systemPromptAppendix;
    private final String workflowVariantKey;
    private final AgentSessionStatus status;
    private final String inputJson;
    private final String finalOutputJson;
    private final String failureReason;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Instant updatedAt;

    private AgentSession(
            UUID id,
            String profileKey,
            String modelKey,
            String task,
            String systemPromptAppendix,
            String workflowVariantKey,
            AgentSessionStatus status,
            String inputJson,
            String finalOutputJson,
            String failureReason,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.profileKey = requireText(profileKey, "profileKey");
        this.modelKey = requireText(modelKey, "modelKey");
        this.task = requireText(task, "task");
        this.systemPromptAppendix = normalizeOptional(systemPromptAppendix);
        this.workflowVariantKey = normalizeOptional(workflowVariantKey);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.inputJson = normalizeOptional(inputJson);
        this.finalOutputJson = normalizeOptional(finalOutputJson);
        this.failureReason = normalizeOptional(failureReason);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static AgentSession createNew(String profileKey, String modelKey, String task, String inputJson, Instant now) {
        return createNew(profileKey, modelKey, task, inputJson, null, null, now);
    }

    public static AgentSession createNew(
            String profileKey,
            String modelKey,
            String task,
            String inputJson,
            String systemPromptAppendix,
            Instant now) {
        return createNew(profileKey, modelKey, task, inputJson, systemPromptAppendix, null, now);
    }

    public static AgentSession createNew(
            String profileKey,
            String modelKey,
            String task,
            String inputJson,
            String systemPromptAppendix,
            String workflowVariantKey,
            Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new AgentSession(
                UUID.randomUUID(),
                profileKey,
                modelKey,
                task,
                systemPromptAppendix,
                workflowVariantKey,
                AgentSessionStatus.CREATED,
                inputJson,
                null,
                null,
                now,
                null,
                null,
                now);
    }

    public static AgentSession reconstitute(
            UUID id,
            String profileKey,
            String modelKey,
            String task,
            String systemPromptAppendix,
            String workflowVariantKey,
            AgentSessionStatus status,
            String inputJson,
            String finalOutputJson,
            String failureReason,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            Instant updatedAt) {
        return new AgentSession(
                id,
                profileKey,
                modelKey,
                task,
                systemPromptAppendix,
                workflowVariantKey,
                status,
                inputJson,
                finalOutputJson,
                failureReason,
                createdAt,
                startedAt,
                finishedAt,
                updatedAt);
    }

    public AgentSession markRunning(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new AgentSession(
                id,
                profileKey,
                modelKey,
                task,
                systemPromptAppendix,
                workflowVariantKey,
                AgentSessionStatus.RUNNING,
                inputJson,
                finalOutputJson,
                null,
                createdAt,
                startedAt == null ? now : startedAt,
                null,
                now);
    }

    public AgentSession complete(String finalOutputJson, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new AgentSession(
                id,
                profileKey,
                modelKey,
                task,
                systemPromptAppendix,
                workflowVariantKey,
                AgentSessionStatus.COMPLETED,
                inputJson,
                finalOutputJson,
                null,
                createdAt,
                startedAt == null ? now : startedAt,
                now,
                now);
    }

    public AgentSession fail(String failureReason, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new AgentSession(
                id,
                profileKey,
                modelKey,
                task,
                systemPromptAppendix,
                workflowVariantKey,
                AgentSessionStatus.FAILED,
                inputJson,
                null,
                requireText(failureReason, "failureReason"),
                createdAt,
                startedAt,
                now,
                now);
    }

    public UUID id() {
        return id;
    }

    public String profileKey() {
        return profileKey;
    }

    public String modelKey() {
        return modelKey;
    }

    public String task() {
        return task;
    }

    public String systemPromptAppendix() {
        return systemPromptAppendix;
    }

    public String workflowVariantKey() {
        return workflowVariantKey;
    }

    public AgentSessionStatus status() {
        return status;
    }

    public String inputJson() {
        return inputJson;
    }

    public String finalOutputJson() {
        return finalOutputJson;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
