package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;

public interface OutputValidationStrategy {
    String key();

    List<OutputValidationIssue> validate(OutputValidationStrategyRequest request);
}
