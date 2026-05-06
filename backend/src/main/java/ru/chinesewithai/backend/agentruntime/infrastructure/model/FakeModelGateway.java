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
    private static final String HSK5_BLUEPRINT_PROFILE_KEY = "lesson-stage:hsk5_v1_blueprint";
    private static final String HSK5_GRAMMAR_PROFILE_KEY = "lesson-stage:hsk5_v1_grammar";
    private static final String HSK5_VOCABULARY_PRACTICE_PROFILE_KEY = "lesson-stage:hsk5_v1_vocabulary_practice";
    private static final String HSK5_WORD_GAME_PROFILE_KEY = "lesson-stage:hsk5_v1_word_game";
    private static final String HSK5_COMPOSER_PROFILE_KEY = "lesson-generator:hsk5_v1_composer";
    private static final String HSK5_V2_SOURCE_NORMALIZER_PROFILE_KEY = "lesson-stage:hsk5_v2_source_normalizer";
    private static final String HSK5_V2_COMPOSER_PROFILE_KEY = "lesson-generator:hsk5_v2_composer";
    private static final String INVALID_OUTPUT_MARKER = "[[INVALID_LESSON_OUTPUT]]";
    private static final String REPAIRABLE_INVALID_OUTPUT_MARKER = "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]";
    private static final String REPAIR_PROMPT_MARKER = "The previous final JSON response was rejected";
    private static final String VOCABULARY_REVIEW_PLAN_MARKER = "### Vocabulary review plan";
    private static final String RETURN_FINAL_ANSWER_MARKER = "\n\nReturn the final answer";
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
        if (HSK5_BLUEPRINT_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5Blueprint(request);
        }
        if (HSK5_GRAMMAR_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5Grammar(request);
        }
        if (HSK5_VOCABULARY_PRACTICE_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5VocabularyPractice(request);
        }
        if (HSK5_WORD_GAME_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5WordGame(request);
        }
        if (HSK5_COMPOSER_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5ComposedLesson(request);
        }
        if (HSK5_V2_SOURCE_NORMALIZER_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5V2SourcePack(request);
        }
        if (HSK5_V2_COMPOSER_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateHsk5V2ComposedLesson(request);
        }

        var toolMessage = request.messages().stream()
                .filter(message ->
                        message.role() == AgentModelMessageRole.TOOL && STATIC_TOOL_NAME.equals(message.name()))
                .findFirst()
                .map(message -> readJson(message.content()).path("toolMessage").asText(null))
                .orElse(null);
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
        if (seenLearnerLevel != null) {
            finalOutputPayload.put("seenLearnerLevel", seenLearnerLevel);
        }
        var finalOutput = writeJson(finalOutputPayload);
        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private AgentModelResponse generateHsk5Blueprint(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var draft = input.path("draft");
        var title = draft.path("title").asText("HSK 5 Lesson");
        var sourceText = readSourceTextFromInput(input, "上传的资料帮助我们学习中文。");
        var reviewWords = extractReviewWords(request);
        var newWords = hsk5NewWords(reviewWords);
        var finalOutput = writeJson(Map.of(
                "title", title,
                "readingText", sourceText,
                "newWords", newWords,
                "reviewWords", reviewWords,
                "grammarPoints", List.of(
                        Map.of("name", "把 focus", "pattern", "把 + object + verb/result"),
                        Map.of("name", "虽然...但是...", "pattern", "虽然 + situation, 但是 + contrast")),
                "lessonTone", "lively and supportive",
                "lessonGoal", "Understand the reading text and reuse the key words in speech."));
        return finalOutput(finalOutput);
    }

    private AgentModelResponse generateHsk5Grammar(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var points = input.path("blueprint").path("grammarPoints");
        var grammarPoints = new ArrayList<Map<String, Object>>();
        if (points.isArray()) {
            for (var point : points) {
                grammarPoints.add(Map.of(
                        "name", point.path("name").asText("grammar point"),
                        "pattern", point.path("pattern").asText("pattern"),
                        "explanation", "Use this pattern to make the HSK5 reading more precise.",
                        "examples", List.of(Map.of(
                                "sentence", "虽然情况很复杂，但是我们仍然保持清楚的态度。",
                                "translation", "Although the situation is complex, we still maintain a clear attitude.",
                                "notes", List.of("Notice the contrast between the two clauses."))),
                        "exercises", List.of(Map.of(
                                "prompt", "Use this pattern to describe one change in your recent life.",
                                "answerHint", "Start with 虽然 or 把.",
                                "sampleAnswer", "虽然任务很复杂，但是我会一步一步完成。"))));
            }
        }
        var finalOutput = writeJson(Map.of(
                "grammarSections", List.of(Map.of(
                        "type", "grammar",
                        "title", "语法练习",
                        "points", grammarPoints))));
        return finalOutput(finalOutput);
    }

    private AgentModelResponse generateHsk5VocabularyPractice(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var blueprint = input.path("blueprint");
        var sections = new ArrayList<Map<String, Object>>();
        appendWordStudySections(sections, blueprint.path("newWords"), "new");
        appendWordStudySections(sections, blueprint.path("reviewWords"), "review");
        var finalOutput = writeJson(Map.of("sections", sections));
        return finalOutput(finalOutput);
    }

    private AgentModelResponse generateHsk5WordGame(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var newWords = toWordMaps(input.path("blueprint").path("newWords"));
        var reviewWords = toWordMaps(input.path("blueprint").path("reviewWords"));
        var finalOutput = writeJson(Map.of("section", Map.of(
                "type", "word_game",
                "title", "词语快配对",
                "instructions", "根据提示说出正确的词，然后自己造一个短句。",
                "rounds", hsk5GameRounds(newWords, reviewWords))));
        return finalOutput(finalOutput);
    }

    private AgentModelResponse generateHsk5ComposedLesson(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var blueprint = input.path("blueprint");
        var sourceText = readSourceTextFromInput(input, blueprint.path("readingText").asText("上传的资料帮助我们学习中文。"));
        var isRepairAttempt = request.messages().stream()
                .anyMatch(message -> message.role() == AgentModelMessageRole.USER
                        && message.content() != null
                        && message.content().contains(REPAIR_PROMPT_MARKER));

        final String finalOutput;
        if (sourceText.contains(INVALID_OUTPUT_MARKER)) {
            finalOutput = writeJson(invalidHsk5ComposedLesson(input, blueprint));
        } else if (sourceText.contains(REPAIRABLE_INVALID_OUTPUT_MARKER) && !isRepairAttempt) {
            finalOutput = writeJson(invalidHsk5ComposedLesson(input, blueprint));
        } else {
            finalOutput = writeJson(validHsk5ComposedLesson(input, blueprint, sourceText));
        }
        return finalOutput(finalOutput);
    }

    private AgentModelResponse generateHsk5V2SourcePack(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var sources = new ArrayList<Map<String, Object>>();
        var sourceRefs = new ArrayList<Map<String, Object>>();
        var combined = new StringBuilder();

        for (var source : input.path("sourceBundle").path("sources")) {
            var mediaCategory = source.path("mediaCategory").asText("text");
            var normalizedText = source.path("textContent").asText("").trim();
            var warnings = new ArrayList<String>();
            if (normalizedText.isBlank() && "image".equals(mediaCategory)) {
                normalizedText = "Text extracted from image source " + source.path("originalFileName").asText("image");
            }
            if (normalizedText.isBlank() && "pdf".equals(mediaCategory)) {
                warnings.add("PDF source has no extracted text in fake model.");
            }
            if (!normalizedText.isBlank()) {
                if (!combined.isEmpty()) {
                    combined.append("\n\n");
                }
                combined.append(normalizedText);
            }

            var item = new LinkedHashMap<String, Object>();
            item.put("sourceId", source.path("sourceId").asText());
            item.put("position", source.path("position").asInt());
            item.put("mediaCategory", mediaCategory);
            item.put("originalFileName", source.path("originalFileName").asText(null));
            item.put("normalizedText", normalizedText);
            item.put("warnings", warnings);
            sources.add(item);

            sourceRefs.add(Map.of(
                    "sourceId", source.path("sourceId").asText(),
                    "position", source.path("position").asInt(),
                    "label", "source-" + source.path("position").asInt()));
        }

        return finalOutput(writeJson(Map.of(
                "sourcePackVersion", 1,
                "sources", sources,
                "combinedText", combined.toString(),
                "sourceRefs", sourceRefs)));
    }

    private AgentModelResponse generateHsk5V2ComposedLesson(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var sourcePack = input.path("sourcePack");
        var draft = input.path("draft");
        var combinedText = sourcePack.path("combinedText").asText("");
        if (combinedText.isBlank()) {
            combinedText = "No normalized source text was produced.";
        }

        var finalOutput = writeJson(Map.of(
                "schemaVersion", 1,
                "moduleKey", "hsk5_v2",
                "title", draft.path("title").asText("HSK 5 v2 source lesson"),
                "studyLanguage", "zh",
                "explanationLanguage", draft.path("explanationLanguage").asText("ru"),
                "translationLanguage", draft.path("translationLanguage").asText("ru"),
                "newWords", List.of(),
                "reviewWords", List.of(),
                "sourcePack", toMap(sourcePack),
                "sections", List.of(
                        Map.of(
                                "type", "source_pack_summary",
                                "title", "Source pack",
                                "sourceCount", sourcePack.path("sources").size()),
                        Map.of(
                                "type", "text",
                                "title", "Normalized source text",
                                "text", combinedText))));
        return finalOutput(finalOutput);
    }

    private Map<String, Object> invalidHsk5ComposedLesson(JsonNode input, JsonNode blueprint) {
        return Map.of(
                "schemaVersion", 1,
                "moduleKey", "hsk5_v1",
                "title", blueprint.path("title").asText("HSK 5 Lesson"),
                "studyLanguage", "zh",
                "explanationLanguage", input.path("draft").path("explanationLanguage").asText("zh"),
                "translationLanguage", input.path("draft").path("translationLanguage").asText("en"),
                "reviewWords", toWordMaps(blueprint.path("reviewWords")),
                "newWords", toWordMaps(blueprint.path("newWords")),
                "sections", List.of(Map.of("type", "text", "title", "Broken", "text", "not the draft text")));
    }

    private Map<String, Object> validHsk5ComposedLesson(JsonNode input, JsonNode blueprint, String sourceText) {
        var sections = new ArrayList<Map<String, Object>>();
        sections.addAll(toSectionMaps(input.path("vocabularyPractice").path("sections")));
        sections.addAll(toSectionMaps(input.path("grammar").path("grammarSections")));
        sections.add(Map.of(
                "type", "text",
                "title", "短文",
                "text", sourceText,
                "readingPrompt", "请先大声读一遍这段文字。",
                "discussionPrompts", List.of("这段话的中心意思是什么？", "哪一个词最值得记住？为什么？")));
        sections.add(Map.of(
                "type", "conversation",
                "title", "聊一聊",
                "mode", "free_talk",
                "prompt", "请用今天的词语说说你最近遇到的一个变化。",
                "followUpPrompts", List.of("你为什么这样想？", "这个变化带来了什么影响？")));
        sections.add(toMap(input.path("wordGame").path("section")));
        return Map.of(
                "schemaVersion", 1,
                "moduleKey", "hsk5_v1",
                "title", blueprint.path("title").asText("HSK 5 Lesson"),
                "studyLanguage", "zh",
                "explanationLanguage", input.path("draft").path("explanationLanguage").asText("zh"),
                "translationLanguage", input.path("draft").path("translationLanguage").asText("en"),
                "reviewWords", toWordMaps(blueprint.path("reviewWords")),
                "newWords", toWordMaps(blueprint.path("newWords")),
                "sections", sections);
    }

    private void appendWordStudySections(List<Map<String, Object>> sections, JsonNode words, String vocabularyStatus) {
        for (var word : toWordMaps(words)) {
            sections.add(hsk5WordStudy(word, vocabularyStatus));
        }
    }

    private List<Map<String, Object>> toWordMaps(JsonNode words) {
        if (words == null || !words.isArray()) {
            return List.of();
        }
        var result = new ArrayList<Map<String, Object>>();
        for (var word : words) {
            result.add(word(
                    word.path("word").asText("词语"),
                    word.path("pinyin").asText("ciyu"),
                    word.path("translation").asText("word")));
        }
        return List.copyOf(result);
    }

    private List<Map<String, Object>> toSectionMaps(JsonNode sections) {
        if (sections == null || !sections.isArray()) {
            return List.of();
        }
        var result = new ArrayList<Map<String, Object>>();
        for (var section : sections) {
            result.add(toMap(section));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, Map.class);
    }

    private AgentModelResponse finalOutput(String finalOutput) {
        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private AgentModelResponse generateTestModuleLesson(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var draft = input.path("draft");
        var explanationLanguage = draft.path("explanationLanguage").asText("zh");
        var translationLanguage = draft.path("translationLanguage").asText("en");
        var title = draft.path("title").asText("Test Lesson");
        var sourceText = readSourceTextFromInput(input, "上传的资料帮助我们学习中文。");
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
        var sourceText = readSourceTextFromInput(input, "上传的资料帮助我们学习中文。");
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

    private String readSourceTextFromInput(JsonNode input, String fallbackText) {
        var sourceText = input.path("sourceText").asText("").trim();
        if (!sourceText.isBlank()) {
            return sourceText;
        }

        var sources = input.path("draft").path("sources");
        if (sources.isArray() && !sources.isEmpty()) {
            var textContent = sources.get(0).path("textContent").asText("").trim();
            if (!textContent.isBlank()) {
                return textContent;
            }
        }

        if (fallbackText != null && !fallbackText.isBlank()) {
            return fallbackText.trim();
        }
        return "上传的资料帮助我们学习中文。";
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
