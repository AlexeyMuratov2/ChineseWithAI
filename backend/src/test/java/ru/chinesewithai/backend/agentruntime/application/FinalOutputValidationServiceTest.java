package ru.chinesewithai.backend.agentruntime.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategy;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;
import ru.chinesewithai.backend.agentruntime.application.service.FinalOutputValidationService;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;
import ru.chinesewithai.backend.agentruntime.infrastructure.validation.DefaultOutputValidator;
import ru.chinesewithai.backend.agentruntime.infrastructure.validation.OutputValidationStrategyCatalog;

class FinalOutputValidationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void reportsInvalidJsonBeforeRunningContractChecks() {
        var service = new FinalOutputValidationService(
                objectMapper, new DefaultOutputValidator(), new OutputValidationStrategyCatalog(List.of()));

        var result = service.validate(profile(), session(), "{not-json");

        assertThat(result.isValid()).isFalse();
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.code()).isEqualTo("invalid_json");
            assertThat(issue.path()).isEqualTo("$");
        });
    }

    @Test
    void combinesBuiltInAndMatchingStrategyValidationIssues() {
        var unsupportedStrategy = new OutputValidationStrategy() {
            @Override
            public boolean supports(OutputValidationStrategyRequest request) {
                return false;
            }

            @Override
            public List<OutputValidationIssue> validate(OutputValidationStrategyRequest request) {
                throw new AssertionError("Unsupported strategies must not run");
            }
        };
        var strategy = new OutputValidationStrategy() {
            @Override
            public boolean supports(OutputValidationStrategyRequest request) {
                return request.profile().profileKey().equals("assistant:v1")
                        && request.profile().outputContract().hasAllRequiredFields(Map.of(
                                "summary", OutputFieldType.STRING,
                                "answer", OutputFieldType.STRING));
            }

            @Override
            public List<OutputValidationIssue> validate(OutputValidationStrategyRequest request) {
                return List.of(new OutputValidationIssue(
                        "custom", "invalid_value", "sections[1].text", "non-empty string", "missing",
                        "sections[1].text must be a non-empty string"));
            }
        };
        var service = new FinalOutputValidationService(
                objectMapper,
                new DefaultOutputValidator(),
                new OutputValidationStrategyCatalog(List.of(unsupportedStrategy, strategy)));

        var result = service.validate(profile(), session(), "{\"summary\":\"ok\"}");

        assertThat(result.isValid()).isFalse();
        assertThat(result.issues()).extracting(OutputValidationIssue::code)
                .containsExactly("missing_field", "invalid_value");
    }

    private AgentProfile profile() {
        return new AgentProfile(
                "assistant:v1",
                "Assistant",
                "Return JSON",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of(
                        "summary", OutputFieldType.STRING,
                        "answer", OutputFieldType.STRING)),
                false,
                true);
    }

    private AgentSession session() {
        return AgentSession.createNew(
                "assistant:v1",
                "fake-model",
                "Do it",
                "{\"question\":\"hi\"}",
                Instant.now());
    }
}
