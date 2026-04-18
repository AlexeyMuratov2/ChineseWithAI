package ru.chinesewithai.backend.agentruntime.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

class DefaultOutputValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultOutputValidator validator = new DefaultOutputValidator();

    @Test
    void reportsMissingRequiredFields() throws Exception {
        var output = objectMapper.readTree("{\"summary\":\"ok\"}");

        var issues = validator.validate(output, new OutputContract(Map.of(
                "summary", OutputFieldType.STRING,
                "answer", OutputFieldType.STRING)));

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().code()).isEqualTo("missing_field");
        assertThat(issues.getFirst().path()).isEqualTo("answer");
    }

    @Test
    void reportsInvalidFieldTypes() throws Exception {
        var output = objectMapper.readTree("{\"answer\":42}");

        var issues = validator.validate(output, new OutputContract(Map.of("answer", OutputFieldType.STRING)));

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().code()).isEqualTo("invalid_type");
        assertThat(issues.getFirst().expected()).isEqualTo("string");
        assertThat(issues.getFirst().actual()).isEqualTo("number");
    }
}
