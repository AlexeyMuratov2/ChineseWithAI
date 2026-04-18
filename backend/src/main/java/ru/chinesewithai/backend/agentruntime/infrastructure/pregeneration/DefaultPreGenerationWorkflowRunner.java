package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileConfigurationException;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentWorkflowVariantNotFoundException;
import ru.chinesewithai.backend.agentruntime.application.exception.PreGenerationWorkflowExecutionException;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationState;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepCatalog;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflow;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowExecutionResult;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowRegistry;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowRunner;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowStepExecution;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

@Component
public class DefaultPreGenerationWorkflowRunner implements PreGenerationWorkflowRunner {

    private final PreGenerationWorkflowRegistry workflowRegistry;
    private final PreGenerationStepCatalog stepCatalog;

    public DefaultPreGenerationWorkflowRunner(
            PreGenerationWorkflowRegistry workflowRegistry, PreGenerationStepCatalog stepCatalog) {
        this.workflowRegistry = workflowRegistry;
        this.stepCatalog = stepCatalog;
    }

    @Override
    public PreGenerationWorkflowExecutionResult run(AgentProfile profile, AgentSession session) {
        var workflow = resolveWorkflow(profile, session);
        var state = PreGenerationState.empty();
        var stepExecutions = new ArrayList<PreGenerationWorkflowStepExecution>();

        for (var stepDefinition : workflow.steps()) {
            if (!stepDefinition.enabled()) {
                continue;
            }
            if (!stepCatalog.contains(stepDefinition.stepKey())) {
                throw new AgentProfileConfigurationException(
                        "Pre-generation workflow references unknown step: " + stepDefinition.stepKey());
            }

            var step = stepCatalog.getRequired(stepDefinition.stepKey());
            final var result = executeStep(profile, session, state, stepDefinition, step);
            stepExecutions.add(new PreGenerationWorkflowStepExecution(
                    stepDefinition.stepKey(), stepDefinition.params(), result.contextSections(), result.artifacts()));
            state = mergeState(state, result.contextSections(), result.artifacts(), stepDefinition.stepKey());
        }

        return new PreGenerationWorkflowExecutionResult(workflow, stepExecutions, state);
    }

    private PreGenerationWorkflow resolveWorkflow(AgentProfile profile, AgentSession session) {
        if (session.workflowVariantKey() != null) {
            return workflowRegistry
                    .findVariant(profile.profileKey(), session.workflowVariantKey())
                    .orElseThrow(() -> new AgentWorkflowVariantNotFoundException(
                            profile.profileKey(), session.workflowVariantKey()));
        }
        return workflowRegistry.findDefault(profile.profileKey()).orElseGet(() -> PreGenerationWorkflow.empty(profile.profileKey()));
    }

    private ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepResult executeStep(
            AgentProfile profile,
            AgentSession session,
            PreGenerationState state,
            ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationWorkflowStepDefinition stepDefinition,
            ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep step) {
        try {
            return step.execute(new PreGenerationStepRequest(profile, session, stepDefinition.params(), state));
        } catch (RuntimeException ex) {
            throw new PreGenerationWorkflowExecutionException(
                    "Pre-generation step failed [%s]: %s"
                            .formatted(stepDefinition.stepKey(), ex.getMessage() == null ? "unknown error" : ex.getMessage()),
                    ex);
        }
    }

    private PreGenerationState mergeState(
            PreGenerationState state,
            List<ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection> sections,
            java.util.Map<String, JsonNode> emittedArtifacts,
            String stepKey) {
        var mergedSections = new ArrayList<>(state.contextSections());
        mergedSections.addAll(sections);

        var mergedArtifacts = new LinkedHashMap<String, JsonNode>();
        mergedArtifacts.putAll(state.artifacts());
        for (var entry : emittedArtifacts.entrySet()) {
            if (mergedArtifacts.containsKey(entry.getKey())) {
                throw new AgentProfileConfigurationException(
                        "Pre-generation step %s emits duplicate artifact key: %s".formatted(stepKey, entry.getKey()));
            }
            mergedArtifacts.put(entry.getKey(), entry.getValue());
        }
        return new PreGenerationState(mergedSections, mergedArtifacts);
    }
}
