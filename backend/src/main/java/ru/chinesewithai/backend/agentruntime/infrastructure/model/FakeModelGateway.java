package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageRole;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelResponse;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolCall;

@Component
public class FakeModelGateway implements AgentModelGateway {

    static final String MODEL_KEY = "fake-model";
    private static final String PROVIDER_KEY = "fake";
    private static final String STATIC_TOOL_NAME = "get_static_test_data";
    private static final String LESSON_GENERATOR_PROFILE_KEY = "lesson-generator:v1";
    private static final String INVALID_OUTPUT_MARKER = "[[INVALID_LESSON_OUTPUT]]";
    private static final String REPAIRABLE_INVALID_OUTPUT_MARKER = "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]";
    private static final String REPAIR_PROMPT_MARKER = "The previous final JSON response was rejected";
    private static final String VOCABULARY_REVIEW_PLAN_MARKER = "### Vocabulary review plan";
    private static final String RETURN_FINAL_ANSWER_MARKER = "\n\nReturn the final answer";
    private static final java.util.regex.Pattern DISPLAY_NAME_PATTERN =
            java.util.regex.Pattern.compile("displayName:\\s*(.+)");
    private static final java.util.regex.Pattern LEARNER_LEVEL_PATTERN =
            java.util.regex.Pattern.compile("learnerLevel:\\s*(.+)");

    private final ObjectMapper objectMapper;

    public FakeModelGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public List<AgentModelDescriptor> supportedModels() {
        return List.of(new AgentModelDescriptor(MODEL_KEY, "Fake Model", PROVIDER_KEY, false));
    }

    @Override
    public AgentModelResponse generate(AgentModelRequest request) {
        if (LESSON_GENERATOR_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateLesson(request);
        }

        var toolMessage = request.messages().stream()
                .filter(message ->
                        message.role() == AgentModelMessageRole.TOOL && STATIC_TOOL_NAME.equals(message.name()))
                .findFirst()
                .map(message -> readJson(message.content()).path("toolMessage").asText(null))
                .orElse(null);
        var seenDisplayName = extractFirstMatch(request, DISPLAY_NAME_PATTERN);
        var seenLearnerLevel = extractFirstMatch(request, LEARNER_LEVEL_PATTERN);

        if (toolMessage == null) {
            var toolCall = new AgentToolCall("fake-tool-call-1", STATIC_TOOL_NAME, "{}");
            var rawPayload = writeJson(Map.of(
                    "type", "TOOL_CALL",
                    "toolCalls", List.of(Map.of(
                            "id", toolCall.toolCallId(),
                            "toolName", toolCall.toolName(),
                            "arguments", Map.of()))));
            return AgentModelResponse.toolCalls(rawPayload, List.of(toolCall));
        }

        var finalOutputPayload = new java.util.LinkedHashMap<String, Object>();
        finalOutputPayload.put("summary", "Fake agent completed successfully");
        finalOutputPayload.put("toolMessage", toolMessage);
        if (seenDisplayName != null) {
            finalOutputPayload.put("seenDisplayName", seenDisplayName);
        }
        if (seenLearnerLevel != null) {
            finalOutputPayload.put("seenLearnerLevel", seenLearnerLevel);
        }
        var finalOutput = writeJson(finalOutputPayload);
        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private AgentModelResponse generateLesson(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var draft = input.path("draft");
        var explanationLanguage = draft.path("explanationLanguage").asText("zh");
        var translationLanguage = draft.path("translationLanguage").asText("en");
        var title = draft.path("title").asText("Test Lesson");
        var sourceText = draft.path("sources").isArray() && !draft.path("sources").isEmpty()
                ? draft.path("sources").get(0).path("textContent").asText("")
                : "";
        var reviewWords = extractReviewWords(request);
        var isRepairAttempt = request.messages().stream()
                .anyMatch(message -> message.role() == AgentModelMessageRole.USER
                        && message.content() != null
                        && message.content().contains(REPAIR_PROMPT_MARKER));

        final String finalOutput;
        if (sourceText.contains(INVALID_OUTPUT_MARKER)) {
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "TestModule",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "reviewWords", reviewWords,
                    "newWords", List.of(),
                    "sections", List.of(Map.of("type", "reading", "title", "Broken"))));
        } else if (sourceText.contains(REPAIRABLE_INVALID_OUTPUT_MARKER) && !isRepairAttempt) {
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "TestModule",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "reviewWords", reviewWords,
                    "newWords", List.of(),
                    "sections", List.of(Map.of("type", "reading", "title", "Broken"))));
        } else {
            var wordUsageTitle = explanationLanguage.startsWith("zh") ? "先看新词" : "New Words";
            var readingTitle = explanationLanguage.startsWith("zh") ? "短文" : "Reading";
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "TestModule",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "reviewWords", reviewWords,
                    "newWords", List.of(
                            Map.of("word", "认识", "pinyin", "rènshi", "translation", "to know"),
                            Map.of("word", "学习", "pinyin", "xuéxí", "translation", "to study")),
                    "sections", List.of(
                            Map.of(
                                    "type", "word_usage",
                                    "title", wordUsageTitle,
                                    "items", List.of(
                                            Map.of(
                                                    "word", "认识",
                                                    "sentence", "我认识这个老师。",
                                                    "translation", "I know this teacher."),
                                            Map.of(
                                                    "word", "学习",
                                                    "sentence", "我每天学习中文。",
                                                    "translation", "I study Chinese every day."))),
                            Map.of(
                                    "type", "reading",
                                    "title", readingTitle,
                                    "text", "我认识这个老师，所以我每天跟他学习中文。",
                                    "translation", "I know this teacher, so I study Chinese with him every day."))));
        }

        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private List<Map<String, Object>> extractReviewWords(AgentModelRequest request) {
        for (var message : request.messages()) {
            var content = message.content();
            if (content == null || !content.contains(VOCABULARY_REVIEW_PLAN_MARKER)) {
                continue;
            }

            var headerIndex = content.indexOf(VOCABULARY_REVIEW_PLAN_MARKER);
            var jsonStart = content.indexOf('{', headerIndex);
            if (jsonStart < 0) {
                continue;
            }

            var jsonEnd = content.indexOf(RETURN_FINAL_ANSWER_MARKER, jsonStart);
            var rawPlan = (jsonEnd >= 0 ? content.substring(jsonStart, jsonEnd) : content.substring(jsonStart)).trim();
            try {
                return toReviewWords(objectMapper.readTree(rawPlan));
            } catch (JsonProcessingException ignored) {
                // Ignore malformed prompt fragments and keep looking.
            }
        }
        return List.of();
    }

    private List<Map<String, Object>> toReviewWords(JsonNode plan) {
        var reviewWords = new ArrayList<Map<String, Object>>();
        var seen = new java.util.LinkedHashSet<String>();
        appendReviewWords(reviewWords, seen, plan.path("mustReview"));
        appendReviewWords(reviewWords, seen, plan.path("shouldReview"));
        return List.copyOf(reviewWords);
    }

    private void appendReviewWords(List<Map<String, Object>> reviewWords, java.util.Set<String> seen, JsonNode items) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (var item : items) {
            var word = item.path("hanzi").asText(null);
            var pinyin = item.path("pinyin").asText(null);
            var translation = item.path("translation").asText(null);
            if (word == null || word.isBlank() || pinyin == null || pinyin.isBlank() || translation == null
                    || translation.isBlank()) {
                continue;
            }
            var key = word + "|" + pinyin + "|" + translation;
            if (!seen.add(key)) {
                continue;
            }
            var reviewWord = new LinkedHashMap<String, Object>();
            reviewWord.put("word", word);
            reviewWord.put("pinyin", pinyin);
            reviewWord.put("translation", translation);
            reviewWords.add(reviewWord);
        }
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse fake model JSON", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize fake model payload", ex);
        }
    }

    private String extractFirstMatch(AgentModelRequest request, java.util.regex.Pattern pattern) {
        for (var message : request.messages()) {
            if (message.content() == null || message.content().isBlank()) {
                continue;
            }
            var matcher = pattern.matcher(message.content());
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }
}
