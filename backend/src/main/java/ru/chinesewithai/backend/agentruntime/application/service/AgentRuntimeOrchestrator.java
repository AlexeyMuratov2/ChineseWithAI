package ru.chinesewithai.backend.agentruntime.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuildRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentSessionRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentStepRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolExecutionRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidator;
import ru.chinesewithai.backend.agentruntime.application.port.out.ToolRegistry;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.AgentContextBuilderCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.model.AgentModelGatewayCatalog;

@Component
public class AgentRuntimeOrchestrator {

    private final AgentSessionRepository agentSessionRepository;
    private final AgentStepRepository agentStepRepository;
    private final AgentModelGatewayCatalog modelGatewayCatalog;
    private final AgentContextBuilderCatalog contextBuilderCatalog;
    private final ToolRegistry toolRegistry;
    private final OutputValidator outputValidator;
    private final ObjectMapper objectMapper;

    public AgentRuntimeOrchestrator(
            AgentSessionRepository agentSessionRepository,
            AgentStepRepository agentStepRepository,
            AgentModelGatewayCatalog modelGatewayCatalog,
            AgentContextBuilderCatalog contextBuilderCatalog,
            ToolRegistry toolRegistry,
            OutputValidator outputValidator,
            ObjectMapper objectMapper) {
        this.agentSessionRepository = agentSessionRepository;
        this.agentStepRepository = agentStepRepository;
        this.modelGatewayCatalog = modelGatewayCatalog;
        this.contextBuilderCatalog = contextBuilderCatalog;
        this.toolRegistry = toolRegistry;
        this.outputValidator = outputValidator;
        this.objectMapper = objectMapper;
    }

    public AgentSession execute(AgentProfile profile, AgentModelDescriptor model, AgentSession initialSession) {
        var session = initialSession;
        var nextStepIndex = agentStepRepository.findBySessionIdOrderByStepIndex(session.id()).size();
        nextStepIndex = appendStep(
                session,
                nextStepIndex,
                AgentStepType.SESSION_CREATED,
                payload(
                        "profileKey",
                        profile.profileKey(),
                        "modelKey",
                        model.modelKey(),
                        "providerKey",
                        model.providerKey(),
                        "ownerId",
                        session.ownerId().toString(),
                        "status",
                        session.status().name()));

        try {
            session = agentSessionRepository.save(session.markRunning(Instant.now()));
            var gateway = modelGatewayCatalog.getRequiredGateway(model.modelKey());
            var contextBuilder = contextBuilderCatalog.getRequired(profile.contextBuilderKey());
            var toolDefinitions = toolRegistry.getDefinitions(profile.allowedToolNames());
            var conversationHistory = new ArrayList<AgentModelMessage>();

            for (int iteration = 0; iteration < profile.executionPolicy().maxSteps(); iteration++) {
                var messages = contextBuilder.buildContext(new AgentContextBuildRequest(profile, session, conversationHistory));
                nextStepIndex = appendStep(
                        session,
                        nextStepIndex,
                        AgentStepType.CONTEXT_BUILT,
                        payload("iteration", iteration, "messages", messages));
                nextStepIndex = appendStep(
                        session,
                        nextStepIndex,
                        AgentStepType.MODEL_REQUEST,
                        payload(
                                "iteration",
                                iteration,
                                "modelKey",
                                model.modelKey(),
                                "providerKey",
                                model.providerKey(),
                                "messages",
                                messages,
                                "tools",
                                toolDefinitions));

                var modelResponse = gateway.generate(new AgentModelRequest(model, profile, session, messages, toolDefinitions));
                nextStepIndex = appendStep(
                        session,
                        nextStepIndex,
                        AgentStepType.MODEL_RESPONSE,
                        payload(
                                "iteration",
                                iteration,
                                "modelKey",
                                model.modelKey(),
                                "providerKey",
                                model.providerKey(),
                                "responseType",
                                modelResponse.responseType().name(),
                                "payload",
                                readJson(modelResponse.rawPayloadJson())));

                switch (modelResponse.responseType()) {
                    case TOOL_CALL -> {
                        conversationHistory.add(AgentModelMessage.assistantToolCalls(modelResponse.toolCalls()));
                        for (var toolCall : modelResponse.toolCalls()) {
                            if (!profile.allowedToolNames().contains(toolCall.toolName())) {
                                return failSession(
                                        session,
                                        nextStepIndex,
                                        "Tool is not allowed for profile: " + toolCall.toolName());
                            }

                            var tool = toolRegistry.findByName(toolCall.toolName()).orElse(null);
                            if (tool == null) {
                                return failSession(
                                        session,
                                        nextStepIndex,
                                        "Tool is not registered: " + toolCall.toolName());
                            }

                            nextStepIndex = appendStep(
                                    session,
                                    nextStepIndex,
                                    AgentStepType.TOOL_CALL,
                                    payload(
                                            "iteration",
                                            iteration,
                                            "toolCallId",
                                            toolCall.toolCallId(),
                                            "toolName",
                                            toolCall.toolName(),
                                            "arguments",
                                            readJson(toolCall.argumentsJson())));
                            var toolResult =
                                    tool.execute(new AgentToolExecutionRequest(profile, session, toolCall.argumentsJson()));
                            nextStepIndex = appendStep(
                                    session,
                                    nextStepIndex,
                                    AgentStepType.TOOL_RESULT,
                                    payload(
                                            "iteration",
                                            iteration,
                                            "toolCallId",
                                            toolCall.toolCallId(),
                                            "toolName",
                                            toolCall.toolName(),
                                            "result",
                                            readJson(toolResult.resultJson())));
                            conversationHistory.add(AgentModelMessage.tool(
                                    toolCall.toolCallId(), toolCall.toolName(), toolResult.resultJson()));
                        }
                    }
                    case FINAL_OUTPUT -> {
                        outputValidator.validate(modelResponse.finalOutputJson(), profile.outputContract());
                        nextStepIndex = appendStep(
                                session,
                                nextStepIndex,
                                AgentStepType.FINAL_OUTPUT,
                                payload("output", readJson(modelResponse.finalOutputJson())));
                        session = agentSessionRepository.save(
                                session.complete(modelResponse.finalOutputJson(), Instant.now()));
                        appendStep(
                                session,
                                nextStepIndex,
                                AgentStepType.SESSION_COMPLETED,
                                payload("status", session.status().name()));
                        return session;
                    }
                }
            }

            return failSession(session, nextStepIndex, "Execution policy max steps exceeded");
        } catch (RuntimeException ex) {
            return failSession(session, nextStepIndex, failureMessage(ex));
        }
    }

    private int appendStep(AgentSession session, int stepIndex, AgentStepType type, Object payload) {
        agentStepRepository.save(AgentStep.create(session.id(), stepIndex, type, writeJson(payload), Instant.now()));
        return stepIndex + 1;
    }

    private AgentSession failSession(AgentSession session, int nextStepIndex, String reason) {
        var failed = agentSessionRepository.save(session.fail(reason, Instant.now()));
        appendStep(failed, nextStepIndex, AgentStepType.SESSION_FAILED, payload("reason", reason));
        return failed;
    }

    private LinkedHashMap<String, Object> payload(Object... values) {
        var payload = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse runtime JSON payload", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize runtime payload", ex);
        }
    }

    private String failureMessage(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Agent runtime execution failed";
        }
        return ex.getMessage();
    }
}
