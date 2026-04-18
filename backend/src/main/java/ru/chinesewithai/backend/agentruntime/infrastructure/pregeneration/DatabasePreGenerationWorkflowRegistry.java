package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileConfigurationException;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflow;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowRegistry;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowStepDefinition;

@Repository
public class DatabasePreGenerationWorkflowRegistry implements PreGenerationWorkflowRegistry {

    private final SpringDataAgentPreGenerationWorkflowJpaRepository repository;
    private final ObjectMapper objectMapper;

    public DatabasePreGenerationWorkflowRegistry(
            SpringDataAgentPreGenerationWorkflowJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PreGenerationWorkflow> findVariant(String profileKey, String workflowVariantKey) {
        if (workflowVariantKey == null || workflowVariantKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByProfileKeyAndWorkflowVariantKeyAndActiveTrue(profileKey, workflowVariantKey)
                .map(this::toDomain);
    }

    @Override
    public Optional<PreGenerationWorkflow> findDefault(String profileKey) {
        return repository.findByProfileKeyAndWorkflowVariantKeyIsNullAndActiveTrue(profileKey)
                .map(this::toDomain);
    }

    private PreGenerationWorkflow toDomain(AgentPreGenerationWorkflowJpaEntity entity) {
        try {
            var stepsNode = objectMapper.readTree(entity.getStepsJson());
            if (!stepsNode.isArray()) {
                throw new AgentProfileConfigurationException(
                        "Pre-generation workflow steps_json must be an array for profile: " + entity.getProfileKey());
            }
            var steps = new ArrayList<PreGenerationWorkflowStepDefinition>();
            for (var node : stepsNode) {
                steps.add(parseStep(entity.getProfileKey(), node));
            }
            return new PreGenerationWorkflow(entity.getProfileKey(), entity.getWorkflowVariantKey(), List.copyOf(steps));
        } catch (JsonProcessingException ex) {
            throw new AgentProfileConfigurationException(
                    "Failed to parse pre-generation workflow config for profile: " + entity.getProfileKey(), ex);
        }
    }

    private PreGenerationWorkflowStepDefinition parseStep(String profileKey, JsonNode node) {
        if (!node.isObject()) {
            throw new AgentProfileConfigurationException(
                    "Pre-generation workflow step entry must be an object for profile: " + profileKey);
        }
        var stepKey = node.path("stepKey").asText(null);
        var enabled = !node.has("enabled") || node.path("enabled").asBoolean(true);
        var params = node.has("params") ? node.get("params") : JsonNodeFactory.instance.objectNode();
        return new PreGenerationWorkflowStepDefinition(stepKey, enabled, params);
    }
}
