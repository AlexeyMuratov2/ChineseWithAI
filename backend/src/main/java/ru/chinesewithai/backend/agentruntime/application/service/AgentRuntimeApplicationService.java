package ru.chinesewithai.backend.agentruntime.application.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.agentruntime.application.command.GetAgentSessionQuery;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentModelNotFoundException;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileNotFoundException;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentSessionNotFoundException;
import ru.chinesewithai.backend.agentruntime.application.port.in.GetAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.in.ListAgentModelsUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.in.ListAgentProfilesUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelCatalog;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentProfileRegistry;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentSessionRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentStepRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.CurrentAgentOwnerProvider;
import ru.chinesewithai.backend.agentruntime.application.view.AgentModelView;
import ru.chinesewithai.backend.agentruntime.application.view.AgentProfileSummaryView;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;
import ru.chinesewithai.backend.agentruntime.application.view.AgentStepView;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

@Service
public class AgentRuntimeApplicationService
        implements StartAgentSessionUseCase, GetAgentSessionUseCase, ListAgentModelsUseCase, ListAgentProfilesUseCase {

    private final AgentModelCatalog agentModelCatalog;
    private final AgentProfileRegistry agentProfileRegistry;
    private final AgentSessionRepository agentSessionRepository;
    private final AgentStepRepository agentStepRepository;
    private final CurrentAgentOwnerProvider currentAgentOwnerProvider;
    private final AgentRuntimeOrchestrator orchestrator;

    public AgentRuntimeApplicationService(
            AgentModelCatalog agentModelCatalog,
            AgentProfileRegistry agentProfileRegistry,
            AgentSessionRepository agentSessionRepository,
            AgentStepRepository agentStepRepository,
            CurrentAgentOwnerProvider currentAgentOwnerProvider,
            AgentRuntimeOrchestrator orchestrator) {
        this.agentModelCatalog = agentModelCatalog;
        this.agentProfileRegistry = agentProfileRegistry;
        this.agentSessionRepository = agentSessionRepository;
        this.agentStepRepository = agentStepRepository;
        this.currentAgentOwnerProvider = currentAgentOwnerProvider;
        this.orchestrator = orchestrator;
    }

    @Override
    public AgentSessionView startSession(StartAgentSessionCommand command) {
        var profile = agentProfileRegistry
                .findByProfileKey(command.profileKey())
                .orElseThrow(() -> new AgentProfileNotFoundException(command.profileKey()));
        var model = agentModelCatalog
                .findByModelKey(command.modelKey())
                .orElseThrow(() -> new AgentModelNotFoundException(command.modelKey()));
        var ownerId = currentAgentOwnerProvider.getCurrentOwnerId();
        var session = agentSessionRepository.save(AgentSession.createNew(
                ownerId,
                profile.profileKey(),
                model.modelKey(),
                command.task(),
                command.inputJson(),
                command.systemPromptAppendix(),
                Instant.now()));
        return toView(orchestrator.execute(profile, model, session));
    }

    @Override
    @Transactional(readOnly = true)
    public AgentSessionView getSession(GetAgentSessionQuery query) {
        var ownerId = currentAgentOwnerProvider.getCurrentOwnerId();
        var session = agentSessionRepository
                .findByIdAndOwnerId(query.sessionId(), ownerId)
                .orElseThrow(() -> new AgentSessionNotFoundException(query.sessionId()));
        return toView(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentModelView> listModels() {
        return agentModelCatalog.findVisibleModels().stream()
                .map(model -> new AgentModelView(model.modelKey(), model.displayName(), model.providerKey()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentProfileSummaryView> listProfiles() {
        return agentProfileRegistry.findVisibleProfiles().stream()
                .map(profile -> new AgentProfileSummaryView(profile.profileKey(), profile.displayName()))
                .toList();
    }

    private AgentSessionView toView(AgentSession session) {
        var steps = agentStepRepository.findBySessionIdOrderByStepIndex(session.id()).stream()
                .map(step -> new AgentStepView(
                        step.id(), step.stepIndex(), step.type().name(), step.payloadJson(), step.createdAt()))
                .toList();

        return new AgentSessionView(
                session.id(),
                session.ownerId(),
                session.profileKey(),
                session.modelKey(),
                session.task(),
                session.status().name(),
                session.inputJson(),
                session.finalOutputJson(),
                session.failureReason(),
                session.createdAt(),
                session.startedAt(),
                session.finishedAt(),
                session.updatedAt(),
                steps);
    }
}
