package ru.chinesewithai.backend.agentruntime.infrastructure.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuildRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuilder;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;

@Component
public class DefaultAgentContextBuilder implements AgentContextBuilder {

    private final ObjectMapper objectMapper;

    public DefaultAgentContextBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "default";
    }

    @Override
    public List<AgentModelMessage> buildContext(AgentContextBuildRequest request) {
        var messages = new ArrayList<AgentModelMessage>();
        messages.add(AgentModelMessage.system(buildSystemPrompt(request)));
        messages.add(AgentModelMessage.user(buildUserPrompt(request)));

        if (!request.profile().memoryPolicy().includePreviousSteps()) {
            return List.copyOf(messages);
        }

        var history = request.conversationHistory();
        var maxHistory = request.profile().memoryPolicy().maxStepHistoryEntries();
        var startIndex = Math.max(0, history.size() - maxHistory);
        messages.addAll(history.subList(startIndex, history.size()));
        return List.copyOf(messages);
    }

    private String buildSystemPrompt(AgentContextBuildRequest request) {
        var joiner = new StringJoiner("\n\n");
        joiner.add(request.profile().systemPrompt());
        if (request.session().systemPromptAppendix() != null) {
            joiner.add(request.session().systemPromptAppendix());
        }
        var systemSections = request.preGenerationState().contextSections().stream()
                .filter(section -> section.target() == PreGenerationContextSectionTarget.SYSTEM)
                .toList();
        if (!systemSections.isEmpty()) {
            joiner.add("Pre-generated system context:\n" + renderSections(systemSections));
        }
        joiner.add("Return the final answer as a valid JSON object without markdown fences.");
        joiner.add("Required output fields:\n" + describeOutputContract(request.profile().outputContract().requiredFields()));
        if (!request.profile().allowedToolNames().isEmpty()) {
            joiner.add("Use available tools when they help produce a better final JSON answer.");
        }
        return joiner.toString();
    }

    private String buildUserPrompt(AgentContextBuildRequest request) {
        var joiner = new StringJoiner("\n\n");
        joiner.add("Task:\n" + request.session().task());
        if (request.session().inputJson() != null) {
            joiner.add("Additional input:\n" + prettyPrintJson(request.session().inputJson()));
        }
        var userSections = request.preGenerationState().contextSections().stream()
                .filter(section -> section.target() == PreGenerationContextSectionTarget.USER)
                .toList();
        if (!userSections.isEmpty()) {
            joiner.add("Pre-generated user context:\n" + renderSections(userSections));
        }
        return joiner.toString();
    }

    private String describeOutputContract(Map<String, ?> requiredFields) {
        var joiner = new StringJoiner("\n");
        requiredFields.forEach((fieldName, fieldType) -> joiner.add("- " + fieldName + ": " + fieldType));
        return joiner.toString();
    }

    private String prettyPrintJson(String rawJson) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(rawJson));
        } catch (JsonProcessingException ex) {
            return rawJson;
        }
    }

    private String renderSections(List<ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection> sections) {
        var joiner = new StringJoiner("\n\n");
        for (var section : sections) {
            joiner.add("### %s\n%s".formatted(section.title(), section.content()));
        }
        return joiner.toString();
    }
}
