package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_sessions")
public class AgentSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "profile_key", nullable = false, length = 120)
    private String profileKey;

    @Column(name = "model_key", nullable = false, length = 120)
    private String modelKey;

    @Column(name = "task", nullable = false)
    private String task;

    @Column(name = "system_prompt_appendix")
    private String systemPromptAppendix;

    @Column(name = "workflow_variant_key", length = 120)
    private String workflowVariantKey;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "input_json")
    private String inputJson;

    @Column(name = "final_output_json")
    private String finalOutputJson;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentSessionJpaEntity() {}

    public AgentSessionJpaEntity(
            UUID id,
            UUID ownerId,
            String profileKey,
            String modelKey,
            String task,
            String systemPromptAppendix,
            String workflowVariantKey,
            String status,
            String inputJson,
            String finalOutputJson,
            String failureReason,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.profileKey = profileKey;
        this.modelKey = modelKey;
        this.task = task;
        this.systemPromptAppendix = systemPromptAppendix;
        this.workflowVariantKey = workflowVariantKey;
        this.status = status;
        this.inputJson = inputJson;
        this.finalOutputJson = finalOutputJson;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public String getModelKey() {
        return modelKey;
    }

    public String getTask() {
        return task;
    }

    public String getSystemPromptAppendix() {
        return systemPromptAppendix;
    }

    public String getWorkflowVariantKey() {
        return workflowVariantKey;
    }

    public String getStatus() {
        return status;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getFinalOutputJson() {
        return finalOutputJson;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
