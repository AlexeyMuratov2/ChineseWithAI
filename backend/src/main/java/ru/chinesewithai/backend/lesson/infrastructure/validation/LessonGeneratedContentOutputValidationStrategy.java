package ru.chinesewithai.backend.lesson.infrastructure.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategy;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.application.port.out.LessonModuleRepository;
import ru.chinesewithai.backend.lesson.application.validation.LessonContentValidator;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

@Component
public class LessonGeneratedContentOutputValidationStrategy implements OutputValidationStrategy {

    static final String STRATEGY_KEY = "lesson-generated-content";
    private static final String VALIDATOR_KEY = "lesson-generated-content";
    private static final Pattern EXACT_VALUE_PATTERN = Pattern.compile("^(.+) must be \"([^\"]+)\"$");
    private static final Pattern SIMPLE_EXPECTATION_PATTERN = Pattern.compile("^(.+) must be (a string|an array|an object|an integer)$");
    private static final Pattern MAX_LENGTH_PATTERN = Pattern.compile("^(.+) must be at most (\\d+) chars$");

    private final LessonContentValidator lessonContentValidator;
    private final LessonModuleRepository lessonModuleRepository;
    private final ObjectMapper objectMapper;

    public LessonGeneratedContentOutputValidationStrategy(
            LessonContentValidator lessonContentValidator,
            LessonModuleRepository lessonModuleRepository,
            ObjectMapper objectMapper) {
        this.lessonContentValidator = lessonContentValidator;
        this.lessonModuleRepository = lessonModuleRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return STRATEGY_KEY;
    }

    @Override
    public List<OutputValidationIssue> validate(OutputValidationStrategyRequest request) {
        try {
            lessonContentValidator.validate(request.rawOutputJson(), resolveModule(request));
            return List.of();
        } catch (LessonContentValidationException ex) {
            return List.of(mapIssue(ex.getMessage(), request.output(), resolveModule(request)));
        }
    }

    private LessonModule resolveModule(OutputValidationStrategyRequest request) {
        var moduleKey = readModuleKey(request.session().inputJson());
        return lessonModuleRepository
                .findByModuleKey(moduleKey)
                .orElseThrow(() -> new IllegalStateException("Missing lesson module for output validation: " + moduleKey));
    }

    private String readModuleKey(String rawInputJson) {
        try {
            var root = objectMapper.readTree(rawInputJson);
            var moduleKey = root == null ? null : root.path("moduleKey").asText(null);
            if (moduleKey == null || moduleKey.isBlank()) {
                throw new IllegalStateException("Lesson generation session is missing moduleKey in inputJson");
            }
            return moduleKey;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Lesson generation session inputJson must be valid JSON", ex);
        }
    }

    private OutputValidationIssue mapIssue(String message, JsonNode output, LessonModule module) {
        if ("Lesson content must be valid JSON".equals(message)) {
            return new OutputValidationIssue(
                    VALIDATOR_KEY, "invalid_json", "$", "valid JSON object", "invalid_json", message);
        }
        if ("Lesson content must be a JSON object".equals(message)) {
            return new OutputValidationIssue(
                    VALIDATOR_KEY, "root_not_object", "$", "object", describeNodeType(output), message);
        }
        if ("moduleKey must be present for module-backed lessons".equals(message)) {
            return new OutputValidationIssue(VALIDATOR_KEY, "missing_field", "moduleKey", "string", "missing", message);
        }
        if ("moduleKey does not match lesson module".equals(message)) {
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_value",
                    "moduleKey",
                    module.moduleKey(),
                    describeActual(output, "moduleKey"),
                    message);
        }
        if ("schemaVersion does not match lesson module".equals(message)) {
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_value",
                    "schemaVersion",
                    String.valueOf(module.schemaVersion()),
                    describeActual(output, "schemaVersion"),
                    message);
        }
        if (message.startsWith(module.moduleKey() + " requires at least one new word")) {
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "empty_array",
                    "newWords",
                    "non-empty array",
                    describeActual(output, "newWords"),
                    message);
        }
        if (message.startsWith(module.moduleKey() + " requires exactly two sections")) {
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_array_size",
                    "sections",
                    "array with exactly two items",
                    describeActual(output, "sections"),
                    message);
        }

        var exactValueMatcher = EXACT_VALUE_PATTERN.matcher(message);
        if (exactValueMatcher.matches()) {
            var path = exactValueMatcher.group(1);
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_value",
                    path,
                    exactValueMatcher.group(2),
                    describeActual(output, path),
                    message);
        }

        var simpleExpectationMatcher = SIMPLE_EXPECTATION_PATTERN.matcher(message);
        if (simpleExpectationMatcher.matches()) {
            var path = simpleExpectationMatcher.group(1);
            var expected = normalizeArticle(simpleExpectationMatcher.group(2));
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_type",
                    path,
                    expected,
                    describeActual(output, path),
                    message);
        }

        var maxLengthMatcher = MAX_LENGTH_PATTERN.matcher(message);
        if (maxLengthMatcher.matches()) {
            var path = maxLengthMatcher.group(1);
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "max_length_exceeded",
                    path,
                    "string with at most %s chars".formatted(maxLengthMatcher.group(2)),
                    describeActual(output, path),
                    message);
        }

        if (message.endsWith(" must not be blank") || message.endsWith(" must not be blank when present")) {
            var path = message.substring(0, message.indexOf(" must not be blank"));
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "blank_value",
                    path,
                    "non-empty string",
                    describeActual(output, path),
                    message);
        }

        if (message.endsWith(" must be > 0")) {
            var path = message.substring(0, message.indexOf(" must be > 0"));
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_value",
                    path,
                    "positive integer",
                    describeActual(output, path),
                    message);
        }

        if (message.endsWith(" must exist in newWords")) {
            var path = message.substring(0, message.indexOf(" must exist in newWords"));
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "invalid_reference",
                    path,
                    "word that exists in newWords",
                    describeActual(output, path),
                    message);
        }

        if (message.endsWith(" must not be empty")) {
            var path = message.substring(0, message.indexOf(" must not be empty"));
            return new OutputValidationIssue(
                    VALIDATOR_KEY,
                    "empty_array",
                    path,
                    "non-empty array",
                    describeActual(output, path),
                    message);
        }

        return new OutputValidationIssue(
                VALIDATOR_KEY, "validation_error", "$", "valid lesson JSON", describeNodeType(output), message);
    }

    private String describeActual(JsonNode root, String path) {
        if (path == null || path.isBlank() || "$".equals(path)) {
            return describeNodeType(root);
        }

        var current = root;
        var token = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            var ch = path.charAt(i);
            if (ch == '.') {
                current = descendObject(current, token.toString());
                token.setLength(0);
                continue;
            }
            if (ch == '[') {
                current = descendObject(current, token.toString());
                token.setLength(0);
                var closeIndex = path.indexOf(']', i);
                if (closeIndex < 0) {
                    return "missing";
                }
                current = descendArray(current, path.substring(i + 1, closeIndex));
                i = closeIndex;
                continue;
            }
            token.append(ch);
        }
        current = descendObject(current, token.toString());
        return describeNodeType(current);
    }

    private JsonNode descendObject(JsonNode current, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return current;
        }
        if (current == null || !current.isObject()) {
            return null;
        }
        return current.get(fieldName);
    }

    private JsonNode descendArray(JsonNode current, String rawIndex) {
        if (current == null || !current.isArray()) {
            return null;
        }
        try {
            var index = Integer.parseInt(rawIndex);
            return index >= 0 && index < current.size() ? current.get(index) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private String normalizeArticle(String expected) {
        return switch (expected) {
            case "a string" -> "string";
            case "an array" -> "array";
            case "an object" -> "object";
            case "an integer" -> "integer";
            default -> expected;
        };
    }
}
