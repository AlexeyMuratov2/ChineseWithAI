package ru.chinesewithai.backend.agentruntime.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.service.OutputRepairPromptFactory;

class OutputRepairPromptFactoryTest {

    private final OutputRepairPromptFactory factory = new OutputRepairPromptFactory(new ObjectMapper());

    @Test
    void rendersReadableRepairPromptWithSerializedIssues() {
        var prompt = factory.buildRepairPrompt(
                2,
                List.of(new OutputValidationIssue(
                        "lesson-generated-content",
                        "missing_field",
                        "sections[1].text",
                        "non-empty string",
                        "missing",
                        "sections[1].text must be a non-empty string")));

        assertThat(prompt).contains("Repair attempt 2 of 3.");
        assertThat(prompt).contains("Return the full corrected JSON object only.");
        assertThat(prompt).contains("\"path\" : \"sections[1].text\"");
        assertThat(prompt).contains("\"code\" : \"missing_field\"");
    }
}
