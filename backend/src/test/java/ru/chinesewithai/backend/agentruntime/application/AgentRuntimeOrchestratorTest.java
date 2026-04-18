package ru.chinesewithai.backend.agentruntime.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelResponse;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentSessionRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentStepRepository;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflow;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowRegistry;
import ru.chinesewithai.backend.agentruntime.application.service.AgentRuntimeOrchestrator;
import ru.chinesewithai.backend.agentruntime.application.service.FinalOutputValidationService;
import ru.chinesewithai.backend.agentruntime.application.service.OutputRepairPromptFactory;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSessionStatus;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStepType;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.AgentContextBuilderCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.context.DefaultAgentContextBuilder;
import ru.chinesewithai.backend.agentruntime.infrastructure.model.AgentModelGatewayCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.model.FakeModelGateway;
import ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration.DefaultPreGenerationWorkflowRunner;
import ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration.SpringPreGenerationStepCatalog;
import ru.chinesewithai.backend.agentruntime.infrastructure.tool.SpringToolRegistry;
import ru.chinesewithai.backend.agentruntime.infrastructure.tool.StaticTestDataTool;
import ru.chinesewithai.backend.agentruntime.infrastructure.validation.DefaultOutputValidator;
import ru.chinesewithai.backend.agentruntime.infrastructure.validation.OutputValidationStrategyCatalog;

class AgentRuntimeOrchestratorTest {

    private static final AgentModelDescriptor FAKE_MODEL =
            new AgentModelDescriptor("fake-model", "Fake Model", "fake", false);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completesExecutionLoopWithToolCallAndFinalOutput() throws Exception {
        var sessionRepository = new InMemoryAgentSessionRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var orchestrator = newOrchestrator(
                sessionRepository,
                stepRepository,
                List.of(new FakeModelGateway(objectMapper)),
                List.of(new StaticTestDataTool(objectMapper)));

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
                false,
                null,
                false);

        var session = sessionRepository.save(AgentSession.createNew(
                UUID.randomUUID(),
                profile.profileKey(),
                FAKE_MODEL.modelKey(),
                "Run a smoke test",
                objectMapper.writeValueAsString(Map.of("objective", "smoke-test")),
                Instant.now()));

        var executed = orchestrator.execute(profile, FAKE_MODEL, session);

        assertThat(executed.status()).isEqualTo(AgentSessionStatus.COMPLETED);
        assertThat(executed.modelKey()).isEqualTo("fake-model");
        assertThat(executed.task()).isEqualTo("Run a smoke test");
        assertThat(objectMapper.readTree(executed.finalOutputJson()).path("toolMessage").asText())
                .isEqualTo("hello-from-static-tool");

        var steps = stepRepository.findBySessionIdOrderByStepIndex(executed.id());
        assertThat(steps).hasSize(13);
        assertThat(steps.stream().map(AgentStep::type).toList())
                .containsExactly(
                        AgentStepType.SESSION_CREATED,
                        AgentStepType.PRE_GENERATION_STARTED,
                        AgentStepType.PRE_GENERATION_COMPLETED,
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

    @Test
    void autoRepairDisabledFailsImmediatelyWhenFinalOutputViolatesContract() throws Exception {
        var sessionRepository = new InMemoryAgentSessionRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var gateway = new InvalidThenRepairableGateway(false);
        var orchestrator = newOrchestrator(
                sessionRepository,
                stepRepository,
                List.of(gateway),
                List.of(new StaticTestDataTool(objectMapper)));

        var profile = new AgentProfile(
                "assistant:v1",
                "Assistant v1",
                "Return JSON.",
                "default",
                List.of("get_static_test_data"),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                new OutputContract(Map.of("answer", OutputFieldType.STRING)),
                false,
                null,
                true);

        var session = sessionRepository.save(AgentSession.createNew(
                UUID.randomUUID(),
                profile.profileKey(),
                FAKE_MODEL.modelKey(),
                "Answer briefly",
                objectMapper.writeValueAsString(Map.of("question", "hi")),
                Instant.now()));

        var executed = orchestrator.execute(profile, FAKE_MODEL, session);

        assertThat(executed.status()).isEqualTo(AgentSessionStatus.FAILED);
        assertThat(executed.finalOutputJson()).isNull();
        assertThat(executed.failureReason()).contains("Missing required output field: answer");
        assertThat(gateway.toolCounts()).containsExactly(1);

        var steps = stepRepository.findBySessionIdOrderByStepIndex(executed.id());
        assertThat(steps.stream().map(AgentStep::type).toList())
                .containsExactly(
                        AgentStepType.SESSION_CREATED,
                        AgentStepType.PRE_GENERATION_STARTED,
                        AgentStepType.PRE_GENERATION_COMPLETED,
                        AgentStepType.CONTEXT_BUILT,
                        AgentStepType.MODEL_REQUEST,
                        AgentStepType.MODEL_RESPONSE,
                        AgentStepType.OUTPUT_VALIDATION_FAILED,
                        AgentStepType.SESSION_FAILED);
    }

    @Test
    void autoRepairEnabledCompletesAfterSuccessfulRetry() throws Exception {
        var sessionRepository = new InMemoryAgentSessionRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var gateway = new InvalidThenRepairableGateway(true);
        var orchestrator = newOrchestrator(
                sessionRepository,
                stepRepository,
                List.of(gateway),
                List.of(new StaticTestDataTool(objectMapper)));

        var profile = new AgentProfile(
                "assistant-repair:v1",
                "Assistant Repair v1",
                "Return JSON.",
                "default",
                List.of("get_static_test_data"),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                new OutputContract(Map.of("answer", OutputFieldType.STRING)),
                true,
                null,
                true);

        var session = sessionRepository.save(AgentSession.createNew(
                UUID.randomUUID(),
                profile.profileKey(),
                FAKE_MODEL.modelKey(),
                "Answer briefly",
                objectMapper.writeValueAsString(Map.of("question", "hi")),
                Instant.now()));

        var executed = orchestrator.execute(profile, FAKE_MODEL, session);

        assertThat(executed.status()).isEqualTo(AgentSessionStatus.COMPLETED);
        assertThat(objectMapper.readTree(executed.finalOutputJson()).path("answer").asText()).isEqualTo("fixed");
        assertThat(gateway.toolCounts()).containsExactly(1, 0);

        var steps = stepRepository.findBySessionIdOrderByStepIndex(executed.id());
        assertThat(steps).extracting(AgentStep::type)
                .containsExactly(
                        AgentStepType.SESSION_CREATED,
                        AgentStepType.PRE_GENERATION_STARTED,
                        AgentStepType.PRE_GENERATION_COMPLETED,
                        AgentStepType.CONTEXT_BUILT,
                        AgentStepType.MODEL_REQUEST,
                        AgentStepType.MODEL_RESPONSE,
                        AgentStepType.OUTPUT_VALIDATION_FAILED,
                        AgentStepType.CONTEXT_BUILT,
                        AgentStepType.MODEL_REQUEST,
                        AgentStepType.MODEL_RESPONSE,
                        AgentStepType.FINAL_OUTPUT,
                        AgentStepType.SESSION_COMPLETED);
    }

    @Test
    void autoRepairEnabledFailsAfterThreeInvalidRepairAttempts() throws Exception {
        var sessionRepository = new InMemoryAgentSessionRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var gateway = new AlwaysInvalidGateway();
        var orchestrator = newOrchestrator(
                sessionRepository,
                stepRepository,
                List.of(gateway),
                List.of(new StaticTestDataTool(objectMapper)));

        var profile = new AgentProfile(
                "assistant-repair:v1",
                "Assistant Repair v1",
                "Return JSON.",
                "default",
                List.of("get_static_test_data"),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                new OutputContract(Map.of("answer", OutputFieldType.STRING)),
                true,
                null,
                true);

        var session = sessionRepository.save(AgentSession.createNew(
                UUID.randomUUID(),
                profile.profileKey(),
                FAKE_MODEL.modelKey(),
                "Answer briefly",
                objectMapper.writeValueAsString(Map.of("question", "hi")),
                Instant.now()));

        var executed = orchestrator.execute(profile, FAKE_MODEL, session);

        assertThat(executed.status()).isEqualTo(AgentSessionStatus.FAILED);
        assertThat(executed.finalOutputJson()).isNull();
        assertThat(executed.failureReason()).contains("after 3 repair attempts");
        assertThat(gateway.toolCounts()).containsExactly(1, 0, 0, 0);

        var steps = stepRepository.findBySessionIdOrderByStepIndex(executed.id());
        assertThat(steps.stream().filter(step -> step.type() == AgentStepType.OUTPUT_VALIDATION_FAILED).toList())
                .hasSize(3);
        assertThat(steps.stream().map(AgentStep::type).toList()).doesNotContain(AgentStepType.FINAL_OUTPUT);
        assertThat(steps.get(steps.size() - 1).type()).isEqualTo(AgentStepType.SESSION_FAILED);
    }

    private AgentRuntimeOrchestrator newOrchestrator(
            InMemoryAgentSessionRepository sessionRepository,
            InMemoryAgentStepRepository stepRepository,
            List<AgentModelGateway> gateways,
            List<ru.chinesewithai.backend.agentruntime.application.port.out.AgentTool> tools) {
        return new AgentRuntimeOrchestrator(
                sessionRepository,
                stepRepository,
                new AgentModelGatewayCatalog(gateways),
                new AgentContextBuilderCatalog(List.of(new DefaultAgentContextBuilder(objectMapper))),
                new SpringToolRegistry(tools),
                new DefaultPreGenerationWorkflowRunner(new EmptyPreGenerationWorkflowRegistry(), new SpringPreGenerationStepCatalog(List.of())),
                new FinalOutputValidationService(
                        objectMapper,
                        new DefaultOutputValidator(),
                        new OutputValidationStrategyCatalog(List.of())),
                new OutputRepairPromptFactory(objectMapper),
                objectMapper);
    }

    private static final class EmptyPreGenerationWorkflowRegistry implements PreGenerationWorkflowRegistry {

        @Override
        public Optional<PreGenerationWorkflow> findVariant(String profileKey, String workflowVariantKey) {
            return Optional.empty();
        }

        @Override
        public Optional<PreGenerationWorkflow> findDefault(String profileKey) {
            return Optional.empty();
        }
    }

    private static final class InvalidThenRepairableGateway implements AgentModelGateway {

        private final boolean repairable;
        private final List<Integer> toolCounts = new ArrayList<>();
        private int callCount;

        private InvalidThenRepairableGateway(boolean repairable) {
            this.repairable = repairable;
        }

        @Override
        public String providerKey() {
            return "fake";
        }

        @Override
        public List<AgentModelDescriptor> supportedModels() {
            return List.of(FAKE_MODEL);
        }

        @Override
        public AgentModelResponse generate(AgentModelRequest request) {
            toolCounts.add(request.tools().size());
            callCount++;
            if (callCount == 1) {
                return finalOutput("{\"summary\":\"wrong\"}");
            }
            if (repairable) {
                return finalOutput("{\"answer\":\"fixed\"}");
            }
            return finalOutput("{\"summary\":\"still-wrong\"}");
        }

        private List<Integer> toolCounts() {
            return List.copyOf(toolCounts);
        }

        private AgentModelResponse finalOutput(String json) {
            return AgentModelResponse.finalOutput("{\"type\":\"FINAL_OUTPUT\"}", json);
        }
    }

    private static final class AlwaysInvalidGateway implements AgentModelGateway {

        private final List<Integer> toolCounts = new ArrayList<>();

        @Override
        public String providerKey() {
            return "fake";
        }

        @Override
        public List<AgentModelDescriptor> supportedModels() {
            return List.of(FAKE_MODEL);
        }

        @Override
        public AgentModelResponse generate(AgentModelRequest request) {
            toolCounts.add(request.tools().size());
            return AgentModelResponse.finalOutput("{\"type\":\"FINAL_OUTPUT\"}", "{\"summary\":\"wrong\"}");
        }

        private List<Integer> toolCounts() {
            return List.copyOf(toolCounts);
        }
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
