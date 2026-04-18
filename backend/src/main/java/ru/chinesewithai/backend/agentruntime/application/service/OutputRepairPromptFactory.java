package ru.chinesewithai.backend.agentruntime.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;

@Component
public class OutputRepairPromptFactory {

    private static final int MAX_REPAIR_ATTEMPTS = 3;

    private final ObjectMapper objectMapper;

    public OutputRepairPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildRepairPrompt(int repairAttempt, List<OutputValidationIssue> issues) {
        var joiner = new StringJoiner("\n\n");
        joiner.add("The previous final JSON response was rejected because it does not satisfy the required output contract.");
        joiner.add("Repair attempt %d of %d.".formatted(repairAttempt, MAX_REPAIR_ATTEMPTS));
        joiner.add("Return the full corrected JSON object only. Do not call tools. Do not add markdown fences. Keep all valid data and change only what is necessary to fix the listed problems.");
        joiner.add("Validation issues:\n" + writeIssues(issues));
        return joiner.toString();
    }

    public String summarizeIssues(List<OutputValidationIssue> issues) {
        if (issues.isEmpty()) {
            return "Output validation failed";
        }
        return issues.stream()
                .limit(3)
                .map(OutputValidationIssue::message)
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private String writeIssues(List<OutputValidationIssue> issues) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(issues);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize output validation issues", ex);
        }
    }
}
