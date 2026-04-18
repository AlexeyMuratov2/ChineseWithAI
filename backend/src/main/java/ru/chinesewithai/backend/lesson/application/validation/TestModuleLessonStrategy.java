package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

@Component
public class TestModuleLessonStrategy implements LessonModuleStrategy {

    private static final String MODULE_KEY = "TestModule";
    private static final String TEXT_NOTE = "TEXT_NOTE";

    @Override
    public String moduleKey() {
        return MODULE_KEY;
    }

    @Override
    public void validateDraftForGeneration(LessonDraftView draft) {
        if (draft.sources().size() != 1) {
            throw new LessonContentValidationException("TestModule requires exactly one TEXT_NOTE draft source");
        }

        var source = draft.sources().getFirst();
        if (!TEXT_NOTE.equals(source.type())) {
            throw new LessonContentValidationException("TestModule supports only TEXT_NOTE draft sources");
        }
        if (source.textContent() == null || source.textContent().isBlank()) {
            throw new LessonContentValidationException("TestModule requires non-empty textContent");
        }
    }

    @Override
    public void validateLesson(JsonNode lessonJson, LessonModule module) {
        requireExactText(lessonJson.get("moduleKey"), "moduleKey", MODULE_KEY);
        requireExactText(lessonJson.get("studyLanguage"), "studyLanguage", "zh");

        var newWords = requireArray(lessonJson.get("newWords"), "newWords");
        if (newWords.isEmpty()) {
            throw new LessonContentValidationException("TestModule requires at least one new word");
        }

        var knownWords = new HashSet<String>();
        for (int i = 0; i < newWords.size(); i++) {
            var wordNode = requireObject(newWords.get(i), "newWords[" + i + "]");
            var word = requireText(wordNode.get("word"), "newWords[" + i + "].word");
            requireText(wordNode.get("pinyin"), "newWords[" + i + "].pinyin");
            requireText(wordNode.get("translation"), "newWords[" + i + "].translation");
            knownWords.add(word);
        }

        var sections = requireArray(lessonJson.get("sections"), "sections");
        if (sections.size() != 2) {
            throw new LessonContentValidationException("TestModule requires exactly two sections");
        }

        validateWordUsageSection(sections.get(0), knownWords);
        validateReadingSection(sections.get(1));
    }

    @Override
    public String generationInstructions() {
        return """
                TestModule rules:
                - The draft contains exactly one TEXT_NOTE source.
                - moduleKey must be "TestModule".
                - studyLanguage must be "zh".
                - Extract the short Chinese reading text from the source and keep it in Chinese.
                - Build newWords as an array of objects with word, pinyin, and translation.
                - sections must have exactly two items in this order:
                  1. word_usage
                  2. reading
                - In word_usage, add example Chinese sentences for every new word and provide translations.
                - In reading, include title, text, and translation.
                """;
    }

    private void validateWordUsageSection(JsonNode node, Set<String> knownWords) {
        var section = requireObject(node, "sections[0]");
        requireExactText(section.get("type"), "sections[0].type", "word_usage");
        requireText(section.get("title"), "sections[0].title");
        var items = requireArray(section.get("items"), "sections[0].items");
        if (items.isEmpty()) {
            throw new LessonContentValidationException("sections[0].items must not be empty");
        }

        for (int i = 0; i < items.size(); i++) {
            var item = requireObject(items.get(i), "sections[0].items[" + i + "]");
            var word = requireText(item.get("word"), "sections[0].items[" + i + "].word");
            if (!knownWords.contains(word)) {
                throw new LessonContentValidationException("sections[0].items[" + i + "].word must exist in newWords");
            }
            requireText(item.get("sentence"), "sections[0].items[" + i + "].sentence");
            requireText(item.get("translation"), "sections[0].items[" + i + "].translation");
        }
    }

    private void validateReadingSection(JsonNode node) {
        var section = requireObject(node, "sections[1]");
        requireExactText(section.get("type"), "sections[1].type", "reading");
        requireText(section.get("title"), "sections[1].title");
        requireText(section.get("text"), "sections[1].text");
        requireText(section.get("translation"), "sections[1].translation");
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

    private static void requireExactText(JsonNode node, String fieldName, String expected) {
        var actual = requireText(node, fieldName);
        if (!expected.equals(actual)) {
            throw new LessonContentValidationException(fieldName + " must be \"" + expected + "\"");
        }
    }
}
