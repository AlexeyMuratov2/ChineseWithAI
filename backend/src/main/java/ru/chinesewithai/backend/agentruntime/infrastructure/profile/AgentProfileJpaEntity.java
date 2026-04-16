package ru.chinesewithai.backend.agentruntime.infrastructure.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "agent_profiles")
public class AgentProfileJpaEntity {

    @Id
    @Column(name = "profile_key", nullable = false, length = 120)
    private String profileKey;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "system_prompt", nullable = false)
    private String systemPrompt;

    @Column(name = "model_key", nullable = false, length = 120)
    private String modelKey;

    @Column(name = "context_builder_key", nullable = false, length = 120)
    private String contextBuilderKey;

    @Column(name = "allowed_tools_json", nullable = false)
    private String allowedToolsJson;

    @Column(name = "execution_policy_json", nullable = false)
    private String executionPolicyJson;

    @Column(name = "memory_policy_json", nullable = false)
    private String memoryPolicyJson;

    @Column(name = "output_contract_json", nullable = false)
    private String outputContractJson;

    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentProfileJpaEntity() {}

    public String getProfileKey() {
        return profileKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getModelKey() {
        return modelKey;
    }

    public String getContextBuilderKey() {
        return contextBuilderKey;
    }

    public String getAllowedToolsJson() {
        return allowedToolsJson;
    }

    public String getExecutionPolicyJson() {
        return executionPolicyJson;
    }

    public String getMemoryPolicyJson() {
        return memoryPolicyJson;
    }

    public String getOutputContractJson() {
        return outputContractJson;
    }

    public boolean isVisible() {
        return visible;
    }
}
