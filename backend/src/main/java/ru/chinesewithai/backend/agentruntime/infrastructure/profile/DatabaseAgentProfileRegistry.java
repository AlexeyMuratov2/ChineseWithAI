package ru.chinesewithai.backend.agentruntime.infrastructure.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileConfigurationException;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentProfileRegistry;
import ru.chinesewithai.backend.agentruntime.application.port.out.ToolRegistry;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.AgentContextBuilderCatalog;

@Repository
public class DatabaseAgentProfileRegistry implements AgentProfileRegistry {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final SpringDataAgentProfileJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final AgentContextBuilderCatalog contextBuilderCatalog;
    private final ToolRegistry toolRegistry;

    public DatabaseAgentProfileRegistry(
            SpringDataAgentProfileJpaRepository repository,
            ObjectMapper objectMapper,
            AgentContextBuilderCatalog contextBuilderCatalog,
            ToolRegistry toolRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.contextBuilderCatalog = contextBuilderCatalog;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Optional<AgentProfile> findByProfileKey(String profileKey) {
        return repository.findById(profileKey).map(this::toValidatedDomain);
    }

    @Override
    public List<AgentProfile> findVisibleProfiles() {
        return repository.findAllByVisibleTrueOrderByDisplayNameAsc().stream()
                .map(this::toValidatedDomain)
                .toList();
    }

    private AgentProfile toValidatedDomain(AgentProfileJpaEntity entity) {
        try {
            var profile = new AgentProfile(
                    entity.getProfileKey(),
                    entity.getDisplayName(),
                    entity.getSystemPrompt(),
                    entity.getContextBuilderKey(),
                    objectMapper.readValue(entity.getAllowedToolsJson(), STRING_LIST),
                    parseExecutionPolicy(entity.getExecutionPolicyJson()),
                    parseMemoryPolicy(entity.getMemoryPolicyJson()),
                    parseOutputContract(entity.getOutputContractJson()),
                    entity.isAutoRepairInvalidOutputEnabled(),
                    entity.isVisible());
            validateRuntimeBindings(profile);
            return profile;
        } catch (JsonProcessingException ex) {
            throw new AgentProfileConfigurationException(
                    "Failed to parse agent profile config for: " + entity.getProfileKey(), ex);
        }
    }

    private ExecutionPolicy parseExecutionPolicy(String rawJson) throws JsonProcessingException {
        var json = objectMapper.readValue(rawJson, ExecutionPolicyJson.class);
        return new ExecutionPolicy(json.maxSteps());
    }

    private MemoryPolicy parseMemoryPolicy(String rawJson) throws JsonProcessingException {
        var json = objectMapper.readValue(rawJson, MemoryPolicyJson.class);
        return new MemoryPolicy(json.includePreviousSteps(), json.maxStepHistoryEntries());
    }

    private OutputContract parseOutputContract(String rawJson) throws JsonProcessingException {
        var json = objectMapper.readValue(rawJson, OutputContractJson.class);
        var requiredFields = new LinkedHashMap<String, OutputFieldType>();
        for (var entry : json.requiredFields().entrySet()) {
            requiredFields.put(entry.getKey(), OutputFieldType.fromValue(entry.getValue()));
        }
        return new OutputContract(requiredFields, rawJson);
    }

    private void validateRuntimeBindings(AgentProfile profile) {
        if (!contextBuilderCatalog.contains(profile.contextBuilderKey())) {
            throw new AgentProfileConfigurationException(
                    "Profile references unknown context builder: " + profile.contextBuilderKey());
        }
        for (var toolName : profile.allowedToolNames()) {
            if (!toolRegistry.isRegistered(toolName)) {
                throw new AgentProfileConfigurationException(
                        "Profile references unknown tool: " + toolName);
            }
        }
    }

    private record ExecutionPolicyJson(int maxSteps) {}

    private record MemoryPolicyJson(boolean includePreviousSteps, int maxStepHistoryEntries) {}

    private record OutputContractJson(Map<String, String> requiredFields) {
        private OutputContractJson {
            requiredFields = requiredFields == null ? Map.of() : Map.copyOf(requiredFields);
        }
    }
}
