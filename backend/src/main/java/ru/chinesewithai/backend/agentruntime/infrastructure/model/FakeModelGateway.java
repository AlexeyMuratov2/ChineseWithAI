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
    private static final String HSK5_LESSON_GENERATOR_PROFILE_KEY = "lesson-generator:hsk5_v1";
    private static final String GRAMMAR_EXERCISE_GENERATOR_PROFILE_KEY = "grammar-exercise-generator:v1";
    private static final String INVALID_OUTPUT_MARKER = "[[INVALID_LESSON_OUTPUT]]";
    private static final String REPAIRABLE_INVALID_OUTPUT_MARKER = "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]";
    private static final String INVALID_GRAMMAR_EXERCISE_OUTPUT_MARKER = "[[INVALID_GRAMMAR_EXERCISE_OUTPUT]]";
    private static final String REPAIRABLE_INVALID_GRAMMAR_EXERCISE_OUTPUT_MARKER =
            "[[REPAIRABLE_INVALID_GRAMMAR_EXERCISE_OUTPUT]]";
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
            return generateTestModuleLesson(request);
        }
        if (HSK5_LESSON_GENERATOR_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5Lesson(request);
        }
        if (GRAMMAR_EXERCISE_GENERATOR_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateGrammarExercise(request);
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

    private AgentModelResponse generateGrammarExercise(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var explanationLanguage = input.path("explanationLanguage").asText("zh");
        var isRepairAttempt = request.messages().stream()
                .anyMatch(message -> message.role() == AgentModelMessageRole.USER
                        && message.content() != null
                        && message.content().contains(REPAIR_PROMPT_MARKER));

        final String finalOutput;
        if (grammarExerciseInputContains(input, INVALID_GRAMMAR_EXERCISE_OUTPUT_MARKER)) {
            finalOutput = writeJson(invalidGrammarExerciseOutput(explanationLanguage));
        } else if (grammarExerciseInputContains(input, REPAIRABLE_INVALID_GRAMMAR_EXERCISE_OUTPUT_MARKER)
                && !isRepairAttempt) {
            finalOutput = writeJson(invalidGrammarExerciseOutput(explanationLanguage));
        } else {
            finalOutput = writeJson(validGrammarExerciseOutput(explanationLanguage));
        }

        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private boolean grammarExerciseInputContains(JsonNode input, String marker) {
        var items = input.path("items");
        if (!items.isArray()) {
            return false;
        }
        for (var item : items) {
            if (item.path("term").asText("").contains(marker)
                    || item.path("focus").asText("").contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> invalidGrammarExerciseOutput(String explanationLanguage) {
        var output = new LinkedHashMap<String, Object>();
        output.put("schemaVersion", 1);
        output.put("explanationLanguage", explanationLanguage);
        output.put("explanations", List.of(grammarExplanation()));
        output.put("usageScenarios", List.of(grammarUsageScenario()));
        output.put("exercises", List.of(completeSentenceExercise()));
        return output;
    }

    private Map<String, Object> validGrammarExerciseOutput(String explanationLanguage) {
        var output = new LinkedHashMap<String, Object>();
        output.put("schemaVersion", 1);
        output.put("explanationLanguage", explanationLanguage);
        output.put("explanations", List.of(grammarExplanation()));
        output.put("usageScenarios", List.of(grammarUsageScenario()));
        output.put("exercises", List.of(completeSentenceExercise(), chooseWordExercise()));
        return output;
    }

    private Map<String, Object> grammarExplanation() {
        return Map.of(
                "title", "yu",
                "targetTerms", List.of("yu"),
                "body", "Use this grammar point to connect a situation with a precise context.");
    }

    private Map<String, Object> grammarUsageScenario() {
        return Map.of(
                "title", "formal context",
                "targetTerms", List.of("yu"),
                "description", "Use the target expression when the sentence needs a compact formal link.",
                "examples", List.of(Map.of(
                        "sentence", "This grammar point appears in a formal sentence.",
                        "translation", "This is a sample translation.",
                        "note", "The fake model keeps content deterministic.")));
    }

    private Map<String, Object> completeSentenceExercise() {
        return Map.of(
                "type", "complete_sentence",
                "title", "Complete the sentence",
                "instruction", "Fill in the blank with a natural expression.",
                "questions", List.of(Map.of(
                        "id", "q1",
                        "prompt", "The report was published ___ Monday.",
                        "answer", "yu",
                        "explanation", "The answer keeps the formal link in place.")));
    }

    private Map<String, Object> chooseWordExercise() {
        return Map.of(
                "type", "choose_word",
                "title", "Choose the better word",
                "instruction", "Choose the word that best fits each sentence.",
                "options", List.of("dating", "xunwen"),
                "questions", List.of(Map.of(
                        "id", "q1",
                        "sentence", "I want to ___ some informal news from a friend.",
                        "answer", "dating",
                        "explanation", "The sentence describes asking around informally.")));
    }

    private AgentModelResponse generateTestModuleLesson(AgentModelRequest request) {
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

    private AgentModelResponse generateHsk5Lesson(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var draft = input.path("draft");
        var explanationLanguage = draft.path("explanationLanguage").asText("zh");
        var translationLanguage = draft.path("translationLanguage").asText("en");
        var title = draft.path("title").asText("HSK 5 Lesson");
        var sourceText = draft.path("sources").isArray() && !draft.path("sources").isEmpty()
                ? draft.path("sources").get(0).path("textContent").asText("").trim()
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
                    "moduleKey", "hsk5_v1",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "reviewWords", reviewWords,
                    "newWords", List.of(),
                    "sections", List.of(Map.of("type", "text", "title", "Broken", "text", "not the draft text"))));
        } else if (sourceText.contains(REPAIRABLE_INVALID_OUTPUT_MARKER) && !isRepairAttempt) {
            var newWords = hsk5NewWords(reviewWords);
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "hsk5_v1",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "reviewWords", reviewWords,
                    "newWords", newWords,
                    "sections", List.of(
                            Map.of("type", "text", "title", "Broken", "text", "not the draft text"),
                            hsk5WordStudy(newWords.getFirst(), "review"))));
        } else {
            var newWords = hsk5NewWords(reviewWords);
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "hsk5_v1",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "reviewWords", reviewWords,
                    "newWords", newWords,
                    "sections", hsk5Sections(sourceText, newWords, reviewWords)));
        }

        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private List<Map<String, Object>> hsk5NewWords(List<Map<String, Object>> reviewWords) {
        var reviewWordValues = reviewWords.stream()
                .map(word -> String.valueOf(word.get("word")))
                .collect(java.util.stream.Collectors.toSet());
        var candidates = List.of(
                word("机会", "jīhuì", "opportunity"),
                word("影响", "yǐngxiǎng", "influence"),
                word("保持", "bǎochí", "to maintain"),
                word("复杂", "fùzá", "complex"),
                word("趋势", "qūshì", "trend"));
        var selected = candidates.stream()
                .filter(word -> !reviewWordValues.contains(word.get("word")))
                .limit(2)
                .toList();
        return selected.isEmpty() ? List.of(word("趋势", "qūshì", "trend")) : selected;
    }

    private List<Map<String, Object>> hsk5Sections(
            String sourceText, List<Map<String, Object>> newWords, List<Map<String, Object>> reviewWords) {
        var sections = new ArrayList<Map<String, Object>>();
        for (var word : newWords) {
            sections.add(hsk5WordStudy(word, "new"));
        }
        for (var word : reviewWords) {
            sections.add(hsk5WordStudy(word, "review"));
        }
        sections.add(Map.of(
                "type", "text",
                "title", "短文",
                "text", sourceText,
                "readingPrompt", "请先大声读一遍这段文字。",
                "discussionPrompts", List.of("你觉得这段话的中心意思是什么？", "哪一个词最值得记住？为什么？")));
        sections.add(Map.of(
                "type", "conversation",
                "title", "聊一聊",
                "mode", "free_talk",
                "prompt", "请用今天的词说说你最近遇到的一个变化。",
                "followUpPrompts", List.of("你为什么这样想？", "这个变化带来了什么影响？")));
        sections.add(Map.of(
                "type", "word_game",
                "title", "词语快配对",
                "instructions", "根据提示说出正确的词，然后自己造一个短句。",
                "rounds", hsk5GameRounds(newWords, reviewWords)));
        return List.copyOf(sections);
    }

    private Map<String, Object> hsk5WordStudy(Map<String, Object> word, String vocabularyStatus) {
        var value = String.valueOf(word.get("word"));
        var item = new LinkedHashMap<String, Object>();
        item.put("type", "word_study");
        item.put("title", "词语：" + value);
        item.put("word", value);
        item.put("pinyin", word.get("pinyin"));
        item.put("translation", word.get("translation"));
        item.put("vocabularyStatus", vocabularyStatus);
        item.put("sentences", List.of(
                Map.of(
                        "sentence", "这个词语“" + value + "”在今天的短文里很重要。",
                        "translation", "The word " + value + " is important in today's short text.",
                        "notes", List.of("注意它在句子中的搭配。")),
                Map.of(
                        "sentence", "请你用“" + value + "”说一个和自己生活有关的句子。",
                        "translation", "Please use " + value + " to say a sentence related to your own life.")));
        return item;
    }

    private List<Map<String, Object>> hsk5GameRounds(
            List<Map<String, Object>> newWords, List<Map<String, Object>> reviewWords) {
        var rounds = new ArrayList<Map<String, Object>>();
        appendHsk5GameRounds(rounds, newWords, "new");
        appendHsk5GameRounds(rounds, reviewWords, "review");
        return List.copyOf(rounds);
    }

    private void appendHsk5GameRounds(
            List<Map<String, Object>> rounds, List<Map<String, Object>> words, String vocabularyStatus) {
        for (var word : words) {
            var value = String.valueOf(word.get("word"));
            rounds.add(Map.of(
                    "prompt", "哪个词可以表达这个意思：" + word.get("translation") + "？",
                    "answerWord", value,
                    "vocabularyStatus", vocabularyStatus));
        }
    }

    private Map<String, Object> word(String word, String pinyin, String translation) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("word", word);
        payload.put("pinyin", pinyin);
        payload.put("translation", translation);
        return payload;
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
