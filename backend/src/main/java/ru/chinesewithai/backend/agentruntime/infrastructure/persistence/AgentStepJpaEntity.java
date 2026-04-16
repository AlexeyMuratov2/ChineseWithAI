package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_steps")
public class AgentStepJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(name = "step_type", nullable = false, length = 40)
    private String stepType;

    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentStepJpaEntity() {}

    public AgentStepJpaEntity(
            UUID id, UUID sessionId, int stepIndex, String stepType, String payloadJson, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.stepIndex = stepIndex;
        this.stepType = stepType;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public String getStepType() {
        return stepType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
