package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileConfigurationException;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationState;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepCatalog;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepResult;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflow;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowRegistry;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowStepDefinition;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

class DefaultPreGenerationWorkflowRunnerTest {

    @Test
    void resolvesExactWorkflowVariantAndExecutesStepsInOrder() {
        var runner = new DefaultPreGenerationWorkflowRunner(
                new StubWorkflowRegistry(
                        Optional.of(new PreGenerationWorkflow(
                                "test-agent:v1",
                                "personalized-smoke",
                                List.of(
                                        new PreGenerationWorkflowStepDefinition("first-step", true, JsonNodeFactory.instance.objectNode()),
                                        new PreGenerationWorkflowStepDefinition("second-step", true, JsonNodeFactory.instance.objectNode())))),
                        Optional.of(new PreGenerationWorkflow(
                                "test-agent:v1",
                                null,
                                List.of(new PreGenerationWorkflowStepDefinition(
                                        "default-step", true, JsonNodeFactory.instance.objectNode()))))),
                new StubStepCatalog(List.of(
                        new StubStep("first-step", request -> new PreGenerationStepResult(
                                List.of(new PreGenerationContextSection(
                                        PreGenerationContextSectionTarget.SYSTEM, "First", "displayName: Alice")),
                                Map.of("first", TextNode.valueOf("alice")))),
                        new StubStep("second-step", request -> {
                            assertThat(request.state().findArtifact("first")).isPresent();
                            return new PreGenerationStepResult(
                                    List.of(new PreGenerationContextSection(
                                            PreGenerationContextSectionTarget.SYSTEM, "Second", "learnerLevel: HSK2")),
                                    Map.of("second", TextNode.valueOf("hsk2")));
                        }),
                        new StubStep("default-step", request -> PreGenerationStepResult.empty()))));

        var result = runner.run(profile(), session("personalized-smoke"));

        assertThat(result.workflow().workflowVariantKey()).isEqualTo("personalized-smoke");
        assertThat(result.stepExecutions()).extracting(step -> step.stepKey()).containsExactly("first-step", "second-step");
        assertThat(result.state().contextSections()).extracting(PreGenerationContextSection::title).containsExactly("First", "Second");
        assertThat(result.state().findArtifact("first")).isPresent();
        assertThat(result.state().findArtifact("second")).isPresent();
    }

    @Test
    void resolvesDefaultWorkflowWhenVariantIsMissing() {
        var runner = new DefaultPreGenerationWorkflowRunner(
                new StubWorkflowRegistry(
                        Optional.empty(),
                        Optional.of(new PreGenerationWorkflow(
                                "test-agent:v1",
                                null,
                                List.of(new PreGenerationWorkflowStepDefinition(
                                        "default-step", true, JsonNodeFactory.instance.objectNode()))))),
                new StubStepCatalog(List.of(new StubStep(
                        "default-step",
                        request -> new PreGenerationStepResult(
                                List.of(new PreGenerationContextSection(
                                        PreGenerationContextSectionTarget.USER, "Default", "learnerLevel: HSK3")),
                                Map.of())))));

        var result = runner.run(profile(), session(null));

        assertThat(result.workflow().workflowVariantKey()).isNull();
        assertThat(result.stepExecutions()).hasSize(1);
        assertThat(result.state().contextSections()).extracting(PreGenerationContextSection::title).containsExactly("Default");
    }

    @Test
    void fallsBackToEmptyWorkflowWhenNoDefaultExists() {
        var runner = new DefaultPreGenerationWorkflowRunner(
                new StubWorkflowRegistry(Optional.empty(), Optional.empty()),
                new StubStepCatalog(List.of()));

        var result = runner.run(profile(), session(null));

        assertThat(result.workflow().steps()).isEmpty();
        assertThat(result.stepExecutions()).isEmpty();
        assertThat(result.state()).isEqualTo(PreGenerationState.empty());
    }

    @Test
    void rejectsUnknownStepKeys() {
        var runner = new DefaultPreGenerationWorkflowRunner(
                new StubWorkflowRegistry(
                        Optional.of(new PreGenerationWorkflow(
                                "test-agent:v1",
                                "variant-a",
                                List.of(new PreGenerationWorkflowStepDefinition(
                                        "missing-step", true, JsonNodeFactory.instance.objectNode())))),
                        Optional.empty()),
                new StubStepCatalog(List.of()));

        assertThatThrownBy(() -> runner.run(profile(), session("variant-a")))
                .isInstanceOf(AgentProfileConfigurationException.class)
                .hasMessageContaining("unknown step");
    }

    @Test
    void rejectsDuplicateArtifactKeys() {
        var runner = new DefaultPreGenerationWorkflowRunner(
                new StubWorkflowRegistry(
                        Optional.of(new PreGenerationWorkflow(
                                "test-agent:v1",
                                "variant-a",
                                List.of(
                                        new PreGenerationWorkflowStepDefinition("first-step", true, JsonNodeFactory.instance.objectNode()),
                                        new PreGenerationWorkflowStepDefinition("second-step", true, JsonNodeFactory.instance.objectNode())))),
                        Optional.empty()),
                new StubStepCatalog(List.of(
                        new StubStep("first-step", request -> new PreGenerationStepResult(List.of(), Map.of("shared", TextNode.valueOf("a")))),
                        new StubStep("second-step", request -> new PreGenerationStepResult(List.of(), Map.of("shared", TextNode.valueOf("b")))))));

        assertThatThrownBy(() -> runner.run(profile(), session("variant-a")))
                .isInstanceOf(AgentProfileConfigurationException.class)
                .hasMessageContaining("duplicate artifact key");
    }

    private AgentProfile profile() {
        return new AgentProfile(
                "test-agent:v1",
                "Test Agent v1",
                "Return JSON.",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                new OutputContract(Map.of("answer", OutputFieldType.STRING)),
                false,
                null,
                false);
    }

    private AgentSession session(String workflowVariantKey) {
        return AgentSession.createNew(
                UUID.randomUUID(),
                "test-agent:v1",
                "fake-model",
                "Do the thing",
                "{\"objective\":\"smoke\"}",
                null,
                workflowVariantKey,
                Instant.now());
    }

    private record StubWorkflowRegistry(
            Optional<PreGenerationWorkflow> exactWorkflow, Optional<PreGenerationWorkflow> defaultWorkflow)
            implements PreGenerationWorkflowRegistry {

        @Override
        public Optional<PreGenerationWorkflow> findVariant(String profileKey, String workflowVariantKey) {
            return exactWorkflow;
        }

        @Override
        public Optional<PreGenerationWorkflow> findDefault(String profileKey) {
            return defaultWorkflow;
        }
    }

    private static final class StubStepCatalog implements PreGenerationStepCatalog {

        private final Map<String, PreGenerationStep> stepsByKey;

        private StubStepCatalog(List<PreGenerationStep> steps) {
            this.stepsByKey = steps.stream().collect(java.util.stream.Collectors.toMap(
                    PreGenerationStep::key, step -> step, (left, right) -> left, java.util.LinkedHashMap::new));
        }

        @Override
        public boolean contains(String stepKey) {
            return stepsByKey.containsKey(stepKey);
        }

        @Override
        public PreGenerationStep getRequired(String stepKey) {
            return stepsByKey.get(stepKey);
        }
    }

    private record StubStep(String key, java.util.function.Function<PreGenerationStepRequest, PreGenerationStepResult> fn)
            implements PreGenerationStep {
        @Override
        public PreGenerationStepResult execute(PreGenerationStepRequest request) {
            return fn.apply(request);
        }
    }
}
