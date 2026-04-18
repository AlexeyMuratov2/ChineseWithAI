package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;

public record OutputValidationResult(List<OutputValidationIssue> issues) {

    public OutputValidationResult {
        Objects.requireNonNull(issues, "issues must not be null");
        issues = List.copyOf(issues);
    }

    public boolean isValid() {
        return issues.isEmpty();
    }
}
