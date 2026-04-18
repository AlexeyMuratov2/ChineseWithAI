package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;

public interface OutputValidator {
    List<OutputValidationIssue> validate(JsonNode output, OutputContract contract);
}
