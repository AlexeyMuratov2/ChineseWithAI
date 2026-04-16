package ru.chinesewithai.backend.agentruntime.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AgentSession {

    private final UUID id;
    private final UUID ownerId;
    private final String profileKey;
    private final String modelKey;
    private final String task;
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
            UUID ownerId,
            String profileKey,
            String modelKey,
            String task,
            AgentSessionStatus status,
            String inputJson,
            String finalOutputJson,
            String failureReason,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.profileKey = requireText(profileKey, "profileKey");
        this.modelKey = requireText(modelKey, "modelKey");
        this.task = requireText(task, "task");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.inputJson = normalizeOptional(inputJson);
        this.finalOutputJson = normalizeOptional(finalOutputJson);
        this.failureReason = normalizeOptional(failureReason);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static AgentSession createNew(
            UUID ownerId, String profileKey, String modelKey, String task, String inputJson, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new AgentSession(
                UUID.randomUUID(),
                ownerId,
                profileKey,
                modelKey,
                task,
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
            UUID ownerId,
            String profileKey,
            String modelKey,
            String task,
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
                ownerId,
                profileKey,
                modelKey,
                task,
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
                ownerId,
                profileKey,
                modelKey,
                task,
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
                ownerId,
                profileKey,
                modelKey,
                task,
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
                ownerId,
                profileKey,
                modelKey,
                task,
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

    public UUID ownerId() {
        return ownerId;
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
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
