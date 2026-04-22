package ru.chinesewithai.backend.grammarexercise.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

class GrammarExerciseOutputValidationStrategyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GrammarExerciseOutputValidationStrategy strategy = new GrammarExerciseOutputValidationStrategy();

    @Test
    void acceptsValidStructure() throws Exception {
        var issues = validate(validOutputJson());

        assertThat(issues).isEmpty();
    }

    @Test
    void rejectsMissingNestedFields() throws Exception {
        var rawOutput = """
                {
                  "schemaVersion": 1,
                  "explanationLanguage": "zh",
                  "explanations": [
                    { "title": "yu", "targetTerms": ["yu"] }
                  ],
                  "usageScenarios": [],
                  "exercises": [
                    {
                      "type": "complete_sentence",
                      "title": "Complete",
                      "instruction": "Fill the blank.",
                      "questions": [
                        { "id": "q1", "prompt": "A ___ B", "answer": "yu", "explanation": "ok" }
                      ]
                    },
                    {
                      "type": "choose_word",
                      "title": "Choose",
                      "instruction": "Pick one.",
                      "options": ["dating", "xunwen"],
                      "questions": [
                        { "id": "q1", "sentence": "A ___ B", "answer": "dating", "explanation": "ok" }
                      ]
                    }
                  ]
                }
                """;

        var issues = validate(rawOutput);

        assertThat(issues).extracting(OutputValidationIssue::path).contains("explanations[0].body");
    }

    @Test
    void rejectsWrongExerciseCount() throws Exception {
        var rawOutput = """
                {
                  "schemaVersion": 1,
                  "explanationLanguage": "zh",
                  "explanations": [],
                  "usageScenarios": [],
                  "exercises": [
                    {
                      "type": "complete_sentence",
                      "title": "Complete",
                      "instruction": "Fill the blank.",
                      "questions": [
                        { "id": "q1", "prompt": "A ___ B", "answer": "yu", "explanation": "ok" }
                      ]
                    }
                  ]
                }
                """;

        var issues = validate(rawOutput);

        assertThat(issues).anySatisfy(issue -> {
            assertThat(issue.code()).isEqualTo("invalid_array_size");
            assertThat(issue.path()).isEqualTo("exercises");
        });
    }

    @Test
    void rejectsWrongExerciseTypeOrder() throws Exception {
        var rawOutput = """
                {
                  "schemaVersion": 1,
                  "explanationLanguage": "zh",
                  "explanations": [],
                  "usageScenarios": [],
                  "exercises": [
                    {
                      "type": "choose_word",
                      "title": "Choose",
                      "instruction": "Pick one.",
                      "options": ["dating", "xunwen"],
                      "questions": [
                        { "id": "q1", "sentence": "A ___ B", "answer": "dating", "explanation": "ok" }
                      ]
                    },
                    {
                      "type": "complete_sentence",
                      "title": "Complete",
                      "instruction": "Fill the blank.",
                      "questions": [
                        { "id": "q1", "prompt": "A ___ B", "answer": "yu", "explanation": "ok" }
                      ]
                    }
                  ]
                }
                """;

        var issues = validate(rawOutput);

        assertThat(issues).extracting(OutputValidationIssue::path)
                .contains("exercises[0].type", "exercises[1].type", "exercises[1].options");
    }

    @Test
    void rejectsNonArrayNestedCollections() throws Exception {
        var rawOutput = """
                {
                  "schemaVersion": 1,
                  "explanationLanguage": "zh",
                  "explanations": [
                    { "title": "yu", "targetTerms": {}, "body": "body" }
                  ],
                  "usageScenarios": [
                    {
                      "title": "Scenario",
                      "targetTerms": ["yu"],
                      "description": "desc",
                      "examples": {}
                    }
                  ],
                  "exercises": [
                    {
                      "type": "complete_sentence",
                      "title": "Complete",
                      "instruction": "Fill the blank.",
                      "questions": {}
                    },
                    {
                      "type": "choose_word",
                      "title": "Choose",
                      "instruction": "Pick one.",
                      "options": "dating",
                      "questions": {}
                    }
                  ]
                }
                """;

        var issues = validate(rawOutput);

        assertThat(issues).extracting(OutputValidationIssue::path)
                .contains(
                        "explanations[0].targetTerms",
                        "usageScenarios[0].examples",
                        "exercises[0].questions",
                        "exercises[1].options",
                        "exercises[1].questions");
    }

    private List<OutputValidationIssue> validate(String rawOutput) throws Exception {
        return strategy.validate(new OutputValidationStrategyRequest(
                profile(),
                "{}",
                objectMapper.readTree(rawOutput),
                rawOutput));
    }

    private String validOutputJson() {
        return """
                {
                  "schemaVersion": 1,
                  "explanationLanguage": "zh",
                  "explanations": [
                    { "title": "yu", "targetTerms": ["yu"], "body": "body" }
                  ],
                  "usageScenarios": [
                    {
                      "title": "Scenario",
                      "targetTerms": ["yu"],
                      "description": "desc",
                      "examples": [
                        { "sentence": "A sentence.", "translation": "Translation.", "note": "Note." }
                      ]
                    }
                  ],
                  "exercises": [
                    {
                      "type": "complete_sentence",
                      "title": "Complete",
                      "instruction": "Fill the blank.",
                      "questions": [
                        { "id": "q1", "prompt": "A ___ B", "answer": "yu", "explanation": "ok" }
                      ]
                    },
                    {
                      "type": "choose_word",
                      "title": "Choose",
                      "instruction": "Pick one.",
                      "options": ["dating", "xunwen"],
                      "questions": [
                        { "id": "q1", "sentence": "A ___ B", "answer": "dating", "explanation": "ok" }
                      ]
                    }
                  ]
                }
                """;
    }

    private AgentProfile profile() {
        return new AgentProfile(
                GrammarExerciseOutputValidationStrategy.PROFILE_KEY,
                "Grammar Exercise Generator",
                "Return JSON",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of(
                        "schemaVersion", OutputFieldType.NUMBER,
                        "explanationLanguage", OutputFieldType.STRING,
                        "explanations", OutputFieldType.ARRAY,
                        "usageScenarios", OutputFieldType.ARRAY,
                        "exercises", OutputFieldType.ARRAY)),
                true,
                false);
    }

    @SuppressWarnings("unused")
    private AgentSession session() {
        return AgentSession.createNew(
                UUID.randomUUID(),
                GrammarExerciseOutputValidationStrategy.PROFILE_KEY,
                "fake-model",
                "Generate",
                "{}",
                Instant.now());
    }
}
