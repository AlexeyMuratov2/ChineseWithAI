package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyWord;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

@Component
public class LessonContentValidator {

    private static final int MAX_TITLE_LENGTH = 160;

    private final ObjectMapper objectMapper;
    private final LessonModuleStrategyCatalog strategyCatalog;

    public LessonContentValidator(ObjectMapper objectMapper, LessonModuleStrategyCatalog strategyCatalog) {
        this.objectMapper = objectMapper;
        this.strategyCatalog = strategyCatalog;
    }

    public String readModuleKeyOrNull(String rawJson) {
        var root = readObject(rawJson);
        return textOrNull(root.get("moduleKey"), "moduleKey");
    }

    public ValidatedLessonPayload validate(String rawJson, LessonModule module) {
        Objects.requireNonNull(rawJson, "rawJson must not be null");
        var root = readObject(rawJson);

        var moduleKey = textOrNull(root.get("moduleKey"), "moduleKey");
        var schemaVersion = requirePositiveInt(root.get("schemaVersion"), "schemaVersion");
        var title = requireText(root.get("title"), "title", MAX_TITLE_LENGTH);
        var studyLanguage = requireLanguage(root.get("studyLanguage"), "studyLanguage");
        var explanationLanguage = requireLanguage(root.get("explanationLanguage"), "explanationLanguage");
        var translationLanguage = requireLanguage(root.get("translationLanguage"), "translationLanguage");
        var newWords = readNewWords(requireArray(root.get("newWords"), "newWords"));
        var reviewWords = root.has("reviewWords")
                ? readWords(requireArray(root.get("reviewWords"), "reviewWords"), "reviewWords")
                : List.<LessonVocabularyWord>of();
        requireArray(root.get("sections"), "sections");

        if (module != null) {
            if (moduleKey == null) {
                throw new LessonContentValidationException("moduleKey must be present for module-backed lessons");
            }
            if (!module.moduleKey().equals(moduleKey)) {
                throw new LessonContentValidationException("moduleKey does not match lesson module");
            }
            if (schemaVersion != module.schemaVersion()) {
                throw new LessonContentValidationException("schemaVersion does not match lesson module");
            }
            strategyCatalog.getRequired(module.moduleKey()).validateLesson(root, module);
        }

        return new ValidatedLessonPayload(
                moduleKey,
                schemaVersion,
                title,
                studyLanguage.value(),
                explanationLanguage.value(),
                translationLanguage.value(),
                newWords,
                reviewWords,
                writeJson(root));
    }

    private java.util.List<LessonVocabularyWord> readNewWords(JsonNode newWordsNode) {
        return readWords(newWordsNode, "newWords");
    }

    private java.util.List<LessonVocabularyWord> readWords(JsonNode wordsNode, String fieldName) {
        var newWords = new ArrayList<LessonVocabularyWord>(wordsNode.size());
        for (int i = 0; i < wordsNode.size(); i++) {
            var wordNode = wordsNode.get(i);
            if (wordNode == null || !wordNode.isObject()) {
                throw new LessonContentValidationException(fieldName + "[" + i + "] must be an object");
            }
            newWords.add(new LessonVocabularyWord(
                    requireText(wordNode.get("word"), fieldName + "[" + i + "].word", 255),
                    requireText(wordNode.get("pinyin"), fieldName + "[" + i + "].pinyin", 255),
                    requireText(wordNode.get("translation"), fieldName + "[" + i + "].translation", 500)));
        }
        return java.util.List.copyOf(newWords);
    }

    private JsonNode readObject(String rawJson) {
        try {
            var root = objectMapper.readTree(rawJson);
            if (root == null || !root.isObject()) {
                throw new LessonContentValidationException("Lesson content must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException ex) {
            throw new LessonContentValidationException("Lesson content must be valid JSON");
        }
    }

    private static String requireText(JsonNode node, String fieldName, int maxLength) {
        if (node == null || !node.isTextual()) {
            throw new LessonContentValidationException(fieldName + " must be a string");
        }
        var normalized = node.asText().trim();
        if (normalized.isBlank()) {
            throw new LessonContentValidationException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new LessonContentValidationException(fieldName + " must be at most " + maxLength + " chars");
        }
        return normalized;
    }

    private static String textOrNull(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new LessonContentValidationException(fieldName + " must be a string when present");
        }
        var normalized = node.asText().trim();
        if (normalized.isBlank()) {
            throw new LessonContentValidationException(fieldName + " must not be blank when present");
        }
        return normalized;
    }

    private static int requirePositiveInt(JsonNode node, String fieldName) {
        if (node == null || !node.canConvertToInt()) {
            throw new LessonContentValidationException(fieldName + " must be an integer");
        }
        var value = node.asInt();
        if (value <= 0) {
            throw new LessonContentValidationException(fieldName + " must be > 0");
        }
        return value;
    }

    private static LanguageTag requireLanguage(JsonNode node, String fieldName) {
        try {
            return LanguageTag.of(requireText(node, fieldName, 35));
        } catch (IllegalArgumentException ex) {
            throw new LessonContentValidationException(ex.getMessage());
        }
    }

    private static JsonNode requireArray(JsonNode node, String fieldName) {
        if (node == null || !node.isArray()) {
            throw new LessonContentValidationException(fieldName + " must be an array");
        }
        return node;
    }

    private String writeJson(JsonNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize lesson JSON", ex);
        }
    }
}
