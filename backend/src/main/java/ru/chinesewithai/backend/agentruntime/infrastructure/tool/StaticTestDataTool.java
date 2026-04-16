package ru.chinesewithai.backend.agentruntime.infrastructure.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentTool;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolExecutionRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolExecutionResult;

@Component
public class StaticTestDataTool implements AgentTool {

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "additionalProperties": false,
              "properties": {}
            }
            """;

    private final ObjectMapper objectMapper;

    public StaticTestDataTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "get_static_test_data";
    }

    @Override
    public String description() {
        return "Returns deterministic static test data for runtime smoke tests.";
    }

    @Override
    public String inputSchemaJson() {
        return INPUT_SCHEMA;
    }

    @Override
    public AgentToolExecutionResult execute(AgentToolExecutionRequest request) {
        return new AgentToolExecutionResult(writeJson(Map.of(
                "toolMessage", "hello-from-static-tool",
                "source", "static-test-tool")));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize static tool payload", ex);
        }
    }
}
