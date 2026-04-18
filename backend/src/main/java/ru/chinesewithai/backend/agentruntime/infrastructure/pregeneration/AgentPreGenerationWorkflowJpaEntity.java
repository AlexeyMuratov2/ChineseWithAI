package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "agent_pre_generation_workflows")
public class AgentPreGenerationWorkflowJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_key", nullable = false, length = 120)
    private String profileKey;

    @Column(name = "workflow_variant_key", length = 120)
    private String workflowVariantKey;

    @Column(name = "steps_json", nullable = false)
    private String stepsJson;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentPreGenerationWorkflowJpaEntity() {}

    public Long getId() {
        return id;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public String getWorkflowVariantKey() {
        return workflowVariantKey;
    }

    public String getStepsJson() {
        return stepsJson;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
