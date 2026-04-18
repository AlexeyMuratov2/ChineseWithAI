package ru.chinesewithai.backend.agentruntime.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.agentruntime.api.dto.AgentModelDescriptorResponse;
import ru.chinesewithai.backend.agentruntime.api.dto.AgentProfileSummaryResponse;
import ru.chinesewithai.backend.agentruntime.api.dto.AgentSessionResponse;
import ru.chinesewithai.backend.agentruntime.api.dto.AgentStepResponse;
import ru.chinesewithai.backend.agentruntime.api.dto.StartAgentSessionRequest;
import ru.chinesewithai.backend.agentruntime.application.command.GetAgentSessionQuery;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.GetAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.in.ListAgentModelsUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.in.ListAgentProfilesUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.view.AgentModelView;
import ru.chinesewithai.backend.agentruntime.application.view.AgentProfileSummaryView;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;
import ru.chinesewithai.backend.config.OpenApiConfig;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/agent-runtime")
public class AgentRuntimeController {

    private final StartAgentSessionUseCase startAgentSessionUseCase;
    private final GetAgentSessionUseCase getAgentSessionUseCase;
    private final ListAgentModelsUseCase listAgentModelsUseCase;
    private final ListAgentProfilesUseCase listAgentProfilesUseCase;
    private final ObjectMapper objectMapper;

    public AgentRuntimeController(
            StartAgentSessionUseCase startAgentSessionUseCase,
            GetAgentSessionUseCase getAgentSessionUseCase,
            ListAgentModelsUseCase listAgentModelsUseCase,
            ListAgentProfilesUseCase listAgentProfilesUseCase,
            ObjectMapper objectMapper) {
        this.startAgentSessionUseCase = startAgentSessionUseCase;
        this.getAgentSessionUseCase = getAgentSessionUseCase;
        this.listAgentModelsUseCase = listAgentModelsUseCase;
        this.listAgentProfilesUseCase = listAgentProfilesUseCase;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/models")
    public List<AgentModelDescriptorResponse> listModels() {
        return listAgentModelsUseCase.listModels().stream().map(this::toResponse).toList();
    }

    @GetMapping("/profiles")
    public List<AgentProfileSummaryResponse> listProfiles() {
        return listAgentProfilesUseCase.listProfiles().stream().map(this::toResponse).toList();
    }

    @PostMapping("/sessions")
    public ResponseEntity<AgentSessionResponse> startSession(@Valid @RequestBody StartAgentSessionRequest request) {
        if (request.input() != null && !request.input().isObject()) {
            throw new IllegalArgumentException("input must be a JSON object");
        }
        var view = startAgentSessionUseCase.startSession(new StartAgentSessionCommand(
                request.profileKey(),
                request.modelKey(),
                request.task(),
                writeJsonOrNull(request.input()),
                null,
                request.workflowVariantKey()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(view));
    }

    @GetMapping("/sessions/{sessionId}")
    public AgentSessionResponse getSession(@PathVariable UUID sessionId) {
        return toResponse(getAgentSessionUseCase.getSession(new GetAgentSessionQuery(sessionId)));
    }

    private AgentSessionResponse toResponse(AgentSessionView view) {
        var steps = view.steps().stream()
                .map(step -> new AgentStepResponse(
                        step.id(), step.stepIndex(), step.type(), readJson(step.payloadJson()), step.createdAt()))
                .toList();
        return new AgentSessionResponse(
                view.sessionId(),
                view.ownerId(),
                view.profileKey(),
                view.modelKey(),
                view.task(),
                view.workflowVariantKey(),
                view.status(),
                readJsonOrNull(view.inputJson()),
                readJsonOrNull(view.finalOutputJson()),
                view.failureReason(),
                view.createdAt(),
                view.startedAt(),
                view.finishedAt(),
                view.updatedAt(),
                steps);
    }

    private AgentModelDescriptorResponse toResponse(AgentModelView view) {
        return new AgentModelDescriptorResponse(view.modelKey(), view.displayName(), view.providerKey());
    }

    private AgentProfileSummaryResponse toResponse(AgentProfileSummaryView view) {
        return new AgentProfileSummaryResponse(view.profileKey(), view.displayName());
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read JSON payload", ex);
        }
    }

    private JsonNode readJsonOrNull(String rawJson) {
        if (rawJson == null) {
            return null;
        }
        return readJson(rawJson);
    }

    private String writeJsonOrNull(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write JSON payload", ex);
        }
    }
}
