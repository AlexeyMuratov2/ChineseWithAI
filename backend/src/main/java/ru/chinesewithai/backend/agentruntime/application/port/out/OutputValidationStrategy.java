package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;

public interface OutputValidationStrategy {
    boolean supports(OutputValidationStrategyRequest request);

    default int order() {
        return 0;
    }

    List<OutputValidationIssue> validate(OutputValidationStrategyRequest request);
}
