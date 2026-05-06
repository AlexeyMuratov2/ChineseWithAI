package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;

@Component
public class Hsk5GeneratedLessonQualityValidator {

    public void validate(JsonNode lessonJson, String expectedReadingText) {
        var sections = requireArray(lessonJson.get("sections"), "sections");
        if (expectedReadingText == null || expectedReadingText.isBlank()) {
            requireTextSection(sections);
        } else {
            requireExactTextSection(sections, expectedReadingText);
        }
        requireGrammarSection(sections);

        var newWords = readWords(requireArray(lessonJson.get("newWords"), "newWords"), "newWords");
        var reviewWords = readWords(requireArray(lessonJson.get("reviewWords"), "reviewWords"), "reviewWords");
        requireWordStudyCoverage(sections, newWords, "new");
        requireWordStudyCoverage(sections, reviewWords, "review");
        validateGameRounds(sections, union(newWords, reviewWords));
    }

    private void requireExactTextSection(JsonNode sections, String expectedReadingText) {
        for (var section : sections) {
            if ("text".equals(section.path("type").asText()) && expectedReadingText.equals(section.path("text").asText())) {
                return;
            }
        }
        throw new LessonContentValidationException("generated hsk5_v1 lesson must include the exact draft text");
    }

    private void requireTextSection(JsonNode sections) {
        for (var section : sections) {
            if ("text".equals(section.path("type").asText())
                    && section.path("text").isTextual()
                    && !section.path("text").asText().isBlank()) {
                return;
            }
        }
        throw new LessonContentValidationException("generated hsk5_v1 lesson must include a text section from the draft source");
    }

    private void requireGrammarSection(JsonNode sections) {
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            if ("grammar".equals(section.path("type").asText())) {
                if (!section.path("points").isArray() || section.path("points").isEmpty()) {
                    throw new LessonContentValidationException("generated grammar section must contain points");
                }
                return;
            }
        }
        throw new LessonContentValidationException("generated hsk5_v1 lesson must include a grammar section");
    }

    private void requireWordStudyCoverage(JsonNode sections, Set<String> requiredWords, String vocabularyStatus) {
        if (requiredWords.isEmpty()) {
            return;
        }
        var practiced = new LinkedHashSet<String>();
        for (var section : sections) {
            if ("word_study".equals(section.path("type").asText())
                    && vocabularyStatus.equals(section.path("vocabularyStatus").asText())) {
                practiced.add(section.path("word").asText());
            }
        }
        for (var word : requiredWords) {
            if (!practiced.contains(word)) {
                throw new LessonContentValidationException(
                        "generated hsk5_v1 lesson must include word_study for " + vocabularyStatus + " word: " + word);
            }
        }
    }

    private void validateGameRounds(JsonNode sections, Set<String> knownWords) {
        for (var section : sections) {
            if (!"word_game".equals(section.path("type").asText())) {
                continue;
            }
            var rounds = requireArray(section.get("rounds"), "word_game.rounds");
            for (int i = 0; i < rounds.size(); i++) {
                var round = requireObject(rounds.get(i), "word_game.rounds[" + i + "]");
                var answerWord = requireText(round.get("answerWord"), "word_game.rounds[" + i + "].answerWord");
                if (!knownWords.contains(answerWord)) {
                    throw new LessonContentValidationException(
                            "word_game.rounds[" + i + "].answerWord must exist in newWords or reviewWords");
                }
            }
        }
    }

    private Set<String> readWords(JsonNode words, String path) {
        var result = new LinkedHashSet<String>();
        for (int i = 0; i < words.size(); i++) {
            result.add(requireText(requireObject(words.get(i), path + "[" + i + "]").get("word"),
                    path + "[" + i + "].word"));
        }
        return Set.copyOf(result);
    }

    private Set<String> union(Set<String> left, Set<String> right) {
        var result = new LinkedHashSet<String>();
        result.addAll(left);
        result.addAll(right);
        return Set.copyOf(result);
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
}
