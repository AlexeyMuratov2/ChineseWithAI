package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;

public record PreGenerationWorkflowExecutionResult(
        PreGenerationWorkflow workflow,
        List<PreGenerationWorkflowStepExecution> stepExecutions,
        PreGenerationState state) {

    public PreGenerationWorkflowExecutionResult {
        Objects.requireNonNull(workflow, "workflow must not be null");
        Objects.requireNonNull(stepExecutions, "stepExecutions must not be null");
        Objects.requireNonNull(state, "state must not be null");
        stepExecutions = List.copyOf(stepExecutions);
    }
}
