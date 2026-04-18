package ru.chinesewithai.backend.agentruntime.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuildRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentSessionRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentStepRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolExecutionRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationResult;
import ru.chinesewithai.backend.agentruntime.application.port.out.ToolRegistry;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.AgentContextBuilderCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.model.AgentModelGatewayCatalog;

@Component
public class AgentRuntimeOrchestrator {

    private static final int MAX_REPAIR_ATTEMPTS = 3;

    private final AgentSessionRepository agentSessionRepository;
    private final AgentStepRepository agentStepRepository;
    private final AgentModelGatewayCatalog modelGatewayCatalog;
    private final AgentContextBuilderCatalog contextBuilderCatalog;
    private final ToolRegistry toolRegistry;
    private final FinalOutputValidationService finalOutputValidationService;
    private final OutputRepairPromptFactory outputRepairPromptFactory;
    private final ObjectMapper objectMapper;

    public AgentRuntimeOrchestrator(
            AgentSessionRepository agentSessionRepository,
            AgentStepRepository agentStepRepository,
            AgentModelGatewayCatalog modelGatewayCatalog,
            AgentContextBuilderCatalog contextBuilderCatalog,
            ToolRegistry toolRegistry,
            FinalOutputValidationService finalOutputValidationService,
            OutputRepairPromptFactory outputRepairPromptFactory,
            ObjectMapper objectMapper) {
        this.agentSessionRepository = agentSessionRepository;
        this.agentStepRepository = agentStepRepository;
        this.modelGatewayCatalog = modelGatewayCatalog;
        this.contextBuilderCatalog = contextBuilderCatalog;
        this.toolRegistry = toolRegistry;
        this.finalOutputValidationService = finalOutputValidationService;
        this.outputRepairPromptFactory = outputRepairPromptFactory;
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
                        payload("iteration", iteration, "phase", "execution", "messages", messages));
                nextStepIndex = appendStep(
                        session,
                        nextStepIndex,
                        AgentStepType.MODEL_REQUEST,
                        payload(
                                "iteration",
                                iteration,
                                "phase",
                                "execution",
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
                        var validation =
                                finalOutputValidationService.validate(profile, session, modelResponse.finalOutputJson());
                        if (validation.isValid()) {
                            return completeSession(session, nextStepIndex, modelResponse.finalOutputJson());
                        }
                        return repairInvalidFinalOutput(
                                session,
                                profile,
                                model,
                                gateway,
                                contextBuilder,
                                conversationHistory,
                                iteration,
                                nextStepIndex,
                                modelResponse.finalOutputJson(),
                                validation);
                    }
                }
            }

            return failSession(session, nextStepIndex, "Execution policy max steps exceeded");
        } catch (RuntimeException ex) {
            return failSession(session, nextStepIndex, failureMessage(ex));
        }
    }

    private AgentSession repairInvalidFinalOutput(
            AgentSession session,
            AgentProfile profile,
            AgentModelDescriptor model,
            ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway gateway,
            ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuilder contextBuilder,
            List<AgentModelMessage> conversationHistory,
            int iteration,
            int nextStepIndex,
            String rejectedOutputRaw,
            OutputValidationResult validation) {
        var currentRejectedOutput = rejectedOutputRaw;
        var currentIssues = validation.issues();

        if (!profile.autoRepairInvalidOutputEnabled()) {
            nextStepIndex = appendOutputValidationFailed(session, nextStepIndex, 1, currentRejectedOutput, currentIssues);
            return failSession(
                    session,
                    nextStepIndex,
                    "Final output validation failed: " + outputRepairPromptFactory.summarizeIssues(currentIssues));
        }

        for (int repairAttempt = 1; repairAttempt <= MAX_REPAIR_ATTEMPTS; repairAttempt++) {
            nextStepIndex =
                    appendOutputValidationFailed(session, nextStepIndex, repairAttempt, currentRejectedOutput, currentIssues);
            conversationHistory.add(AgentModelMessage.assistant(currentRejectedOutput));
            conversationHistory.add(
                    AgentModelMessage.user(outputRepairPromptFactory.buildRepairPrompt(repairAttempt, currentIssues)));

            var repairMessages = contextBuilder.buildContext(new AgentContextBuildRequest(profile, session, conversationHistory));
            nextStepIndex = appendStep(
                    session,
                    nextStepIndex,
                    AgentStepType.CONTEXT_BUILT,
                    payload(
                            "iteration",
                            iteration,
                            "phase",
                            "repair",
                            "repairAttempt",
                            repairAttempt,
                            "messages",
                            repairMessages));
            nextStepIndex = appendStep(
                    session,
                    nextStepIndex,
                    AgentStepType.MODEL_REQUEST,
                    payload(
                            "iteration",
                            iteration,
                            "phase",
                            "repair",
                            "repairAttempt",
                            repairAttempt,
                            "modelKey",
                            model.modelKey(),
                            "providerKey",
                            model.providerKey(),
                            "messages",
                            repairMessages,
                            "tools",
                            List.of()));

            var repairResponse = gateway.generate(new AgentModelRequest(model, profile, session, repairMessages, List.of()));
            nextStepIndex = appendStep(
                    session,
                    nextStepIndex,
                    AgentStepType.MODEL_RESPONSE,
                    payload(
                            "iteration",
                            iteration,
                            "phase",
                            "repair",
                            "repairAttempt",
                            repairAttempt,
                            "modelKey",
                            model.modelKey(),
                            "providerKey",
                            model.providerKey(),
                            "responseType",
                            repairResponse.responseType().name(),
                            "payload",
                            readJson(repairResponse.rawPayloadJson())));

            if (repairResponse.responseType()
                    == ru.chinesewithai.backend.agentruntime.domain.model.ModelResponseType.TOOL_CALL) {
                currentRejectedOutput = repairResponse.rawPayloadJson();
                currentIssues = List.of(new OutputValidationIssue(
                        "repair-loop",
                        "tool_call_not_allowed",
                        "$",
                        "final JSON object",
                        "tool_call",
                        "Repair response must return only the full JSON object. Tool calls are not allowed during repair."));
                continue;
            }

            var repairValidation =
                    finalOutputValidationService.validate(profile, session, repairResponse.finalOutputJson());
            if (repairValidation.isValid()) {
                return completeSession(session, nextStepIndex, repairResponse.finalOutputJson());
            }

            currentRejectedOutput = repairResponse.finalOutputJson();
            currentIssues = repairValidation.issues();
        }

        return failSession(
                session,
                nextStepIndex,
                "Final output validation failed after %d repair attempts: %s"
                        .formatted(MAX_REPAIR_ATTEMPTS, outputRepairPromptFactory.summarizeIssues(currentIssues)));
    }

    private int appendOutputValidationFailed(
            AgentSession session,
            int nextStepIndex,
            int repairAttempt,
            String rejectedOutputRaw,
            List<OutputValidationIssue> issues) {
        return appendStep(
                session,
                nextStepIndex,
                AgentStepType.OUTPUT_VALIDATION_FAILED,
                payload(
                        "repairAttempt",
                        repairAttempt,
                        "validatorIssues",
                        issues,
                        "rejectedOutputRaw",
                        rejectedOutputRaw));
    }

    private AgentSession completeSession(AgentSession session, int nextStepIndex, String finalOutputJson) {
        nextStepIndex = appendStep(
                session,
                nextStepIndex,
                AgentStepType.FINAL_OUTPUT,
                payload("output", readJson(finalOutputJson)));
        var completed = agentSessionRepository.save(session.complete(finalOutputJson, Instant.now()));
        appendStep(
                completed,
                nextStepIndex,
                AgentStepType.SESSION_COMPLETED,
                payload("status", completed.status().name()));
        return completed;
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
