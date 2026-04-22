package ru.chinesewithai.backend.grammarexercise.infrastructure.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategy;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

@Component
public class GrammarExerciseOutputValidationStrategy implements OutputValidationStrategy {

    public static final String PROFILE_KEY = "grammar-exercise-generator:v1";
    private static final String VALIDATOR_KEY = "grammar-exercise-content";
    private static final Map<String, OutputFieldType> REQUIRED_FIELDS = Map.of(
            "schemaVersion", OutputFieldType.NUMBER,
            "explanationLanguage", OutputFieldType.STRING,
            "explanations", OutputFieldType.ARRAY,
            "usageScenarios", OutputFieldType.ARRAY,
            "exercises", OutputFieldType.ARRAY);

    @Override
    public boolean supports(OutputValidationStrategyRequest request) {
        return PROFILE_KEY.equals(request.profile().profileKey())
                && request.profile().outputContract().hasAllRequiredFields(REQUIRED_FIELDS);
    }

    @Override
    public List<OutputValidationIssue> validate(OutputValidationStrategyRequest request) {
        var issues = new ArrayList<OutputValidationIssue>();
        validateExplanations(request.output(), issues);
        validateUsageScenarios(request.output(), issues);
        validateExercises(request.output(), issues);
        return List.copyOf(issues);
    }

    private void validateExplanations(JsonNode root, List<OutputValidationIssue> issues) {
        var explanations = requireArray(root.get("explanations"), "explanations", issues);
        if (explanations == null) {
            return;
        }
        for (int i = 0; i < explanations.size(); i++) {
            var path = "explanations[" + i + "]";
            var explanation = requireObject(explanations.get(i), path, issues);
            if (explanation == null) {
                continue;
            }
            requireText(explanation.get("title"), path + ".title", issues);
            requireTextArray(explanation.get("targetTerms"), path + ".targetTerms", issues);
            requireText(explanation.get("body"), path + ".body", issues);
        }
    }

    private void validateUsageScenarios(JsonNode root, List<OutputValidationIssue> issues) {
        var scenarios = requireArray(root.get("usageScenarios"), "usageScenarios", issues);
        if (scenarios == null) {
            return;
        }
        for (int i = 0; i < scenarios.size(); i++) {
            var path = "usageScenarios[" + i + "]";
            var scenario = requireObject(scenarios.get(i), path, issues);
            if (scenario == null) {
                continue;
            }
            requireText(scenario.get("title"), path + ".title", issues);
            requireTextArray(scenario.get("targetTerms"), path + ".targetTerms", issues);
            requireText(scenario.get("description"), path + ".description", issues);
            validateExamples(scenario.get("examples"), path + ".examples", issues);
        }
    }

    private void validateExamples(JsonNode examplesNode, String path, List<OutputValidationIssue> issues) {
        var examples = requireArray(examplesNode, path, issues);
        if (examples == null) {
            return;
        }
        for (int i = 0; i < examples.size(); i++) {
            var examplePath = path + "[" + i + "]";
            var example = requireObject(examples.get(i), examplePath, issues);
            if (example == null) {
                continue;
            }
            requireText(example.get("sentence"), examplePath + ".sentence", issues);
            requireOptionalText(example.get("translation"), examplePath + ".translation", issues);
            requireOptionalText(example.get("note"), examplePath + ".note", issues);
        }
    }

    private void validateExercises(JsonNode root, List<OutputValidationIssue> issues) {
        var exercises = requireArray(root.get("exercises"), "exercises", issues);
        if (exercises == null) {
            return;
        }
        if (exercises.size() != 2) {
            issues.add(issue(
                    "invalid_array_size",
                    "exercises",
                    "array with exactly two items",
                    "array with " + exercises.size() + " items",
                    "exercises must contain exactly two items"));
        }
        if (exercises.size() > 0) {
            validateExercise(exercises.get(0), 0, "complete_sentence", issues);
        }
        if (exercises.size() > 1) {
            validateExercise(exercises.get(1), 1, "choose_word", issues);
        }
    }

    private void validateExercise(JsonNode exerciseNode, int index, String expectedType, List<OutputValidationIssue> issues) {
        var path = "exercises[" + index + "]";
        var exercise = requireObject(exerciseNode, path, issues);
        if (exercise == null) {
            return;
        }
        requireExactText(exercise.get("type"), path + ".type", expectedType, issues);
        requireText(exercise.get("title"), path + ".title", issues);
        requireText(exercise.get("instruction"), path + ".instruction", issues);
        if ("choose_word".equals(expectedType)) {
            requireNonEmptyTextArray(exercise.get("options"), path + ".options", issues);
        }
        validateQuestions(exercise.get("questions"), path + ".questions", expectedType, issues);
    }

    private void validateQuestions(
            JsonNode questionsNode, String path, String exerciseType, List<OutputValidationIssue> issues) {
        var questions = requireNonEmptyArray(questionsNode, path, issues);
        if (questions == null) {
            return;
        }
        for (int i = 0; i < questions.size(); i++) {
            var questionPath = path + "[" + i + "]";
            var question = requireObject(questions.get(i), questionPath, issues);
            if (question == null) {
                continue;
            }
            requireText(question.get("id"), questionPath + ".id", issues);
            if ("complete_sentence".equals(exerciseType)) {
                requireText(question.get("prompt"), questionPath + ".prompt", issues);
            } else {
                requireText(question.get("sentence"), questionPath + ".sentence", issues);
            }
            requireText(question.get("answer"), questionPath + ".answer", issues);
            requireText(question.get("explanation"), questionPath + ".explanation", issues);
        }
    }

    private JsonNode requireObject(JsonNode node, String path, List<OutputValidationIssue> issues) {
        if (node != null && node.isObject()) {
            return node;
        }
        issues.add(issue("invalid_type", path, "object", describeNodeType(node), path + " must be an object"));
        return null;
    }

    private JsonNode requireArray(JsonNode node, String path, List<OutputValidationIssue> issues) {
        if (node != null && node.isArray()) {
            return node;
        }
        issues.add(issue("invalid_type", path, "array", describeNodeType(node), path + " must be an array"));
        return null;
    }

    private JsonNode requireNonEmptyArray(JsonNode node, String path, List<OutputValidationIssue> issues) {
        var array = requireArray(node, path, issues);
        if (array != null && array.isEmpty()) {
            issues.add(issue("empty_array", path, "non-empty array", "empty array", path + " must not be empty"));
        }
        return array;
    }

    private void requireTextArray(JsonNode node, String path, List<OutputValidationIssue> issues) {
        var array = requireArray(node, path, issues);
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.size(); i++) {
            requireText(array.get(i), path + "[" + i + "]", issues);
        }
    }

    private void requireNonEmptyTextArray(JsonNode node, String path, List<OutputValidationIssue> issues) {
        var array = requireNonEmptyArray(node, path, issues);
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.size(); i++) {
            requireText(array.get(i), path + "[" + i + "]", issues);
        }
    }

    private void requireText(JsonNode node, String path, List<OutputValidationIssue> issues) {
        if (node != null && node.isTextual()) {
            return;
        }
        issues.add(issue("invalid_type", path, "string", describeNodeType(node), path + " must be a string"));
    }

    private void requireOptionalText(JsonNode node, String path, List<OutputValidationIssue> issues) {
        if (node == null || node.isNull() || node.isTextual()) {
            return;
        }
        issues.add(issue("invalid_type", path, "string", describeNodeType(node), path + " must be a string when present"));
    }

    private void requireExactText(JsonNode node, String path, String expectedValue, List<OutputValidationIssue> issues) {
        if (node == null || !node.isTextual()) {
            issues.add(issue("invalid_type", path, "string", describeNodeType(node), path + " must be a string"));
            return;
        }
        if (!expectedValue.equals(node.asText())) {
            issues.add(issue(
                    "invalid_value",
                    path,
                    expectedValue,
                    node.asText(),
                    path + " must be \"" + expectedValue + "\""));
        }
    }

    private OutputValidationIssue issue(
            String code, String path, String expected, String actual, String message) {
        return new OutputValidationIssue(VALIDATOR_KEY, code, path, expected, actual, message);
    }

    private String describeNodeType(JsonNode node) {
        if (node == null) {
            return "missing";
        }
        if (node.isTextual()) {
            return "string";
        }
        if (node.isNumber()) {
            return "number";
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        if (node.isObject()) {
            return "object";
        }
        if (node.isArray()) {
            return "array";
        }
        if (node.isNull()) {
            return "null";
        }
        return node.getNodeType().name().toLowerCase(java.util.Locale.ROOT);
    }
}
