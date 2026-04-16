package ru.chinesewithai.backend.agentruntime.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentSessionRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentStepRepository;
import ru.chinesewithai.backend.agentruntime.application.service.AgentRuntimeOrchestrator;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSessionStatus;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.AgentContextBuilderCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.DefaultAgentContextBuilder;
import ru.chinesewithai.backend.agentruntime.infrastructure.model.AgentModelGatewayCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.model.FakeModelGateway;
import ru.chinesewithai.backend.agentruntime.infrastructure.tool.SpringToolRegistry;
import ru.chinesewithai.backend.agentruntime.infrastructure.tool.StaticTestDataTool;
import ru.chinesewithai.backend.agentruntime.infrastructure.validation.DefaultOutputValidator;

class AgentRuntimeOrchestratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completesExecutionLoopWithToolCallAndFinalOutput() throws Exception {
        var sessionRepository = new InMemoryAgentSessionRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var orchestrator = new AgentRuntimeOrchestrator(
                sessionRepository,
                stepRepository,
                new AgentModelGatewayCatalog(List.of(new FakeModelGateway(objectMapper))),
                new AgentContextBuilderCatalog(List.of(new DefaultAgentContextBuilder(objectMapper))),
                new SpringToolRegistry(List.of(new StaticTestDataTool(objectMapper))),
                new DefaultOutputValidator(objectMapper),
                objectMapper);

        var profile = new AgentProfile(
                "test-agent:v1",
                "Test Agent v1",
                "Use the tool before returning final output.",
                "default",
                List.of("get_static_test_data"),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                new OutputContract(Map.of(
                        "summary", OutputFieldType.STRING,
                        "toolMessage", OutputFieldType.STRING)),
                false);
        var model = new AgentModelDescriptor("fake-model", "Fake Model", "fake", false);

        var session = sessionRepository.save(AgentSession.createNew(
                UUID.randomUUID(),
                profile.profileKey(),
                model.modelKey(),
                "Run a smoke test",
                objectMapper.writeValueAsString(Map.of("objective", "smoke-test")),
                java.time.Instant.now()));

        var executed = orchestrator.execute(profile, model, session);

        assertThat(executed.status()).isEqualTo(AgentSessionStatus.COMPLETED);
        assertThat(executed.modelKey()).isEqualTo("fake-model");
        assertThat(executed.task()).isEqualTo("Run a smoke test");
        assertThat(objectMapper.readTree(executed.finalOutputJson()).path("toolMessage").asText())
                .isEqualTo("hello-from-static-tool");

        var steps = stepRepository.findBySessionIdOrderByStepIndex(executed.id());
        assertThat(steps).hasSize(11);
        assertThat(steps.stream().map(AgentStep::type).toList())
                .containsExactly(
                        AgentStepType.SESSION_CREATED,
                        AgentStepType.CONTEXT_BUILT,
                        AgentStepType.MODEL_REQUEST,
                        AgentStepType.MODEL_RESPONSE,
                        AgentStepType.TOOL_CALL,
                        AgentStepType.TOOL_RESULT,
                        AgentStepType.CONTEXT_BUILT,
                        AgentStepType.MODEL_REQUEST,
                        AgentStepType.MODEL_RESPONSE,
                        AgentStepType.FINAL_OUTPUT,
                        AgentStepType.SESSION_COMPLETED);
    }

    private static final class InMemoryAgentSessionRepository implements AgentSessionRepository {

        private final Map<UUID, AgentSession> sessions = new LinkedHashMap<>();

        @Override
        public AgentSession save(AgentSession session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<AgentSession> findByIdAndOwnerId(UUID sessionId, UUID ownerId) {
            return Optional.ofNullable(sessions.get(sessionId)).filter(session -> session.ownerId().equals(ownerId));
        }
    }

    private static final class InMemoryAgentStepRepository implements AgentStepRepository {

        private final List<AgentStep> steps = new ArrayList<>();

        @Override
        public AgentStep save(AgentStep step) {
            steps.removeIf(existing -> existing.id().equals(step.id()));
            steps.add(step);
            return step;
        }

        @Override
        public List<AgentStep> findBySessionIdOrderByStepIndex(UUID sessionId) {
            return steps.stream()
                    .filter(step -> step.sessionId().equals(sessionId))
                    .sorted(Comparator.comparingInt(AgentStep::stepIndex))
                    .toList();
        }
    }
}
