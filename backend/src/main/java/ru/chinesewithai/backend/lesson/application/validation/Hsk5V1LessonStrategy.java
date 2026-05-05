package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

@Component
public class Hsk5V1LessonStrategy implements LessonModuleStrategy {

    public static final String MODULE_KEY = "hsk5_v1";

    private static final String TEXT_NOTE = "TEXT_NOTE";
    private static final String SENTENCES_FIELD = "sentences";
    private static final String EXAMPLE_SENTENCES_ALIAS = "exampleSentences";
    private static final String ANSWER_WORD_FIELD = "answerWord";
    private static final String EXPECTED_WORD_ALIAS = "expectedWord";
    private static final Set<String> ALLOWED_SECTION_TYPES =
            Set.of("word_study", "conversation", "text", "word_game", "grammar");
    private static final Set<String> ALLOWED_CONVERSATION_MODES =
            Set.of("make_sentence", "perform", "free_talk");
    private static final Set<String> ALLOWED_VOCABULARY_STATUSES = Set.of("new", "review");

    @Override
    public String moduleKey() {
        return MODULE_KEY;
    }

    @Override
    public void validateDraftForGeneration(LessonDraftView draft) {
        if (draft.sources().size() != 1) {
            throw new LessonContentValidationException("hsk5_v1 requires exactly one TEXT_NOTE draft source");
        }

        var source = draft.sources().getFirst();
        if (!TEXT_NOTE.equals(source.type())) {
            throw new LessonContentValidationException("hsk5_v1 supports only TEXT_NOTE draft sources");
        }
        if (source.textContent() == null || source.textContent().isBlank()) {
            throw new LessonContentValidationException("hsk5_v1 requires non-empty textContent");
        }
    }

    @Override
    public void validateLesson(JsonNode lessonJson, LessonModule module) {
        validateReviewWords(requireArray(lessonJson.get("reviewWords"), "reviewWords"));

        var sections = requireArray(lessonJson.get("sections"), "sections");
        for (int i = 0; i < sections.size(); i++) {
            validateSection(requireObject(sections.get(i), "sections[" + i + "]"), "sections[" + i + "]");
        }
    }

    @Override
    public String generationInstructions() {
        return """
                hsk5_v1 rules:
                - The draft contains exactly one TEXT_NOTE source. Use its textContent as the Chinese reading text.
                - moduleKey must be "hsk5_v1"; schemaVersion must match the module; studyLanguage should be "zh".
                - The learner level is HSK5, so do not translate the reading text. Keep the tone natural and alive.
                - newWords and reviewWords must both be present as arrays of {word, pinyin, translation}.
                - Use review vocabulary from vocabularyReviewPlan when available. Put practiced review words in reviewWords.
                - sections is an array of blocks in any order. Use only these block types: word_study, grammar, conversation, text, word_game.
                - Add a text block with title, text, readingPrompt, and discussionPrompts. Prefer the draft TEXT_NOTE content in text. Do not add a translation field unless it is necessary for a graceful fallback.
                - Add a grammar block with title and points. Each point has name, pattern, explanation, examples, and exercises. Grammar examples use sentence, translation, and optional notes. Grammar exercises use prompt and optional answerHint/sampleAnswer.
                - Add a conversation block with title, mode, prompt, and followUpPrompts. mode should be make_sentence, perform, or free_talk.
                - Add a word_game block with title, instructions, and rounds. Each round should use exactly these field names: prompt, answerWord, vocabularyStatus. Do not use expectedWord.
                - Add word_study blocks for newWords and reviewWords. Use vocabularyStatus "new" or "review".
                - A word_study block should use exactly these field names: type, title, word, pinyin, translation, vocabularyStatus, sentences. Do not use exampleSentences.
                - The sentences field is an array of objects with sentence, translation, and optional notes. Put Chinese example sentences in sentence and translations in translation.
                """;
    }

    private void validateReviewWords(JsonNode reviewWords) {
        for (int i = 0; i < reviewWords.size(); i++) {
            var item = requireObject(reviewWords.get(i), "reviewWords[" + i + "]");
            requireText(item.get("word"), "reviewWords[" + i + "].word");
            requireText(item.get("pinyin"), "reviewWords[" + i + "].pinyin");
            requireText(item.get("translation"), "reviewWords[" + i + "].translation");
        }
    }

    private void validateSection(JsonNode section, String path) {
        var type = requireText(section.get("type"), path + ".type");
        if (!ALLOWED_SECTION_TYPES.contains(type)) {
            throw new LessonContentValidationException(path + ".type has unsupported hsk5_v1 block type");
        }

        switch (type) {
            case "word_study" -> validateWordStudySection(section, path);
            case "conversation" -> validateConversationSection(section, path);
            case "text" -> validateTextSection(section, path);
            case "word_game" -> validateWordGameSection(section, path);
            case "grammar" -> validateGrammarSection(section, path);
            default -> throw new IllegalStateException("Unhandled hsk5_v1 section type: " + type);
        }
    }

    private void validateWordStudySection(JsonNode section, String path) {
        requireText(section.get("title"), path + ".title");
        requireText(section.get("word"), path + ".word");
        requireText(section.get("pinyin"), path + ".pinyin");
        requireText(section.get("translation"), path + ".translation");
        requireOneOf(section.get("vocabularyStatus"), path + ".vocabularyStatus", ALLOWED_VOCABULARY_STATUSES);

        var sentences = requireArrayField(section, path, SENTENCES_FIELD, EXAMPLE_SENTENCES_ALIAS);
        for (int i = 0; i < sentences.size(); i++) {
            var sentence = requireObject(sentences.get(i), path + ".sentences[" + i + "]");
            requireText(sentence.get("sentence"), path + ".sentences[" + i + "].sentence");
            requireText(sentence.get("translation"), path + ".sentences[" + i + "].translation");
            validateOptionalTextArray(sentence.get("notes"), path + ".sentences[" + i + "].notes");
        }
    }

    private void validateConversationSection(JsonNode section, String path) {
        requireText(section.get("title"), path + ".title");
        requireOneOf(section.get("mode"), path + ".mode", ALLOWED_CONVERSATION_MODES);
        requireText(section.get("prompt"), path + ".prompt");
        validateTextArray(requireArray(section.get("followUpPrompts"), path + ".followUpPrompts"), path + ".followUpPrompts");
    }

    private void validateTextSection(JsonNode section, String path) {
        requireText(section.get("title"), path + ".title");
        requireText(section.get("text"), path + ".text");
        requireText(section.get("readingPrompt"), path + ".readingPrompt");
        validateTextArray(requireArray(section.get("discussionPrompts"), path + ".discussionPrompts"), path + ".discussionPrompts");
    }

    private void validateWordGameSection(JsonNode section, String path) {
        requireText(section.get("title"), path + ".title");
        requireText(section.get("instructions"), path + ".instructions");
        var rounds = requireArray(section.get("rounds"), path + ".rounds");
        for (int i = 0; i < rounds.size(); i++) {
            var round = requireObject(rounds.get(i), path + ".rounds[" + i + "]");
            requireText(round.get("prompt"), path + ".rounds[" + i + "].prompt");
            requireTextField(round, path + ".rounds[" + i + "]", ANSWER_WORD_FIELD, EXPECTED_WORD_ALIAS);
            validateOptionalOneOf(
                    round.get("vocabularyStatus"), path + ".rounds[" + i + "].vocabularyStatus", ALLOWED_VOCABULARY_STATUSES);
        }
    }

    private void validateGrammarSection(JsonNode section, String path) {
        requireText(section.get("title"), path + ".title");
        var points = requireArray(section.get("points"), path + ".points");
        if (points.isEmpty()) {
            throw new LessonContentValidationException(path + ".points must not be empty");
        }
        for (int i = 0; i < points.size(); i++) {
            var point = requireObject(points.get(i), path + ".points[" + i + "]");
            requireText(point.get("name"), path + ".points[" + i + "].name");
            requireText(point.get("pattern"), path + ".points[" + i + "].pattern");
            requireText(point.get("explanation"), path + ".points[" + i + "].explanation");
            validateGrammarExamples(requireArray(point.get("examples"), path + ".points[" + i + "].examples"),
                    path + ".points[" + i + "].examples");
            validateGrammarExercises(requireArray(point.get("exercises"), path + ".points[" + i + "].exercises"),
                    path + ".points[" + i + "].exercises");
        }
    }

    private void validateGrammarExamples(JsonNode examples, String path) {
        if (examples.isEmpty()) {
            throw new LessonContentValidationException(path + " must not be empty");
        }
        for (int i = 0; i < examples.size(); i++) {
            var example = requireObject(examples.get(i), path + "[" + i + "]");
            requireText(example.get("sentence"), path + "[" + i + "].sentence");
            requireText(example.get("translation"), path + "[" + i + "].translation");
            validateOptionalTextArray(example.get("notes"), path + "[" + i + "].notes");
        }
    }

    private void validateGrammarExercises(JsonNode exercises, String path) {
        if (exercises.isEmpty()) {
            throw new LessonContentValidationException(path + " must not be empty");
        }
        for (int i = 0; i < exercises.size(); i++) {
            var exercise = requireObject(exercises.get(i), path + "[" + i + "]");
            requireText(exercise.get("prompt"), path + "[" + i + "].prompt");
            validateOptionalText(exercise.get("answerHint"), path + "[" + i + "].answerHint");
            validateOptionalText(exercise.get("sampleAnswer"), path + "[" + i + "].sampleAnswer");
        }
    }

    private void validateOptionalText(JsonNode node, String path) {
        if (node != null && !node.isNull()) {
            requireText(node, path);
        }
    }

    private JsonNode requireArrayField(JsonNode object, String objectPath, String fieldName, String aliasFieldName) {
        var node = object.get(fieldName);
        if ((node == null || node.isNull()) && aliasFieldName != null) {
            node = object.get(aliasFieldName);
            if (node != null && node.isArray() && object instanceof ObjectNode mutableObject) {
                mutableObject.set(fieldName, node);
                mutableObject.remove(aliasFieldName);
                node = mutableObject.get(fieldName);
            }
        }
        return requireArray(node, objectPath + "." + fieldName);
    }

    private String requireTextField(JsonNode object, String objectPath, String fieldName, String aliasFieldName) {
        var node = object.get(fieldName);
        if ((node == null || node.isNull()) && aliasFieldName != null) {
            node = object.get(aliasFieldName);
            if (node != null && node.isTextual() && object instanceof ObjectNode mutableObject) {
                mutableObject.set(fieldName, node);
                mutableObject.remove(aliasFieldName);
                node = mutableObject.get(fieldName);
            }
        }
        return requireText(node, objectPath + "." + fieldName);
    }

    private void validateOptionalTextArray(JsonNode node, String path) {
        if (node == null || node.isNull()) {
            return;
        }
        validateTextArray(requireArray(node, path), path);
    }

    private void validateTextArray(JsonNode array, String path) {
        for (int i = 0; i < array.size(); i++) {
            requireText(array.get(i), path + "[" + i + "]");
        }
    }

    private static JsonNode requireObject(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            throw new LessonContentValidationException(fieldName + " must be an object");
        }
        return node;
    }

    private static JsonNode requireArray(JsonNode node, String fieldName) {
        if (node == null || !node.isArray()) {
            throw new LessonContentValidationException(fieldName + " must be an array");
        }
        return node;
    }

    private static String requireText(JsonNode node, String fieldName) {
        if (node == null || !node.isTextual()) {
            throw new LessonContentValidationException(fieldName + " must be a string");
        }
        var value = node.asText().trim();
        if (value.isBlank()) {
            throw new LessonContentValidationException(fieldName + " must not be blank");
        }
        return value;
    }

    private static void requireOneOf(JsonNode node, String fieldName, Set<String> allowedValues) {
        var value = requireText(node, fieldName);
        if (!allowedValues.contains(value)) {
            throw new LessonContentValidationException(fieldName + " must be one of " + String.join(", ", allowedValues));
        }
    }

    private static void validateOptionalOneOf(JsonNode node, String fieldName, Set<String> allowedValues) {
        if (node == null || node.isNull()) {
            return;
        }
        requireOneOf(node, fieldName, allowedValues);
    }
}
