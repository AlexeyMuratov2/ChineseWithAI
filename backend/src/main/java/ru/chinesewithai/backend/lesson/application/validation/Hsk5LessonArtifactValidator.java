package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;

@Component
public class Hsk5LessonArtifactValidator {

    public void validateBlueprint(JsonNode artifact, String expectedReadingText) {
        requireObject(artifact, "blueprint");
        requireText(artifact.get("title"), "blueprint.title");
        var readingText = requireText(artifact.get("readingText"), "blueprint.readingText");
        if (expectedReadingText != null && !expectedReadingText.equals(readingText)) {
            throw new LessonContentValidationException("blueprint.readingText must match the draft text exactly");
        }
        validateWordArray(requireArray(artifact.get("newWords"), "blueprint.newWords"), "blueprint.newWords");
        validateWordArray(requireArray(artifact.get("reviewWords"), "blueprint.reviewWords"), "blueprint.reviewWords");
        var grammarPoints = requireArray(artifact.get("grammarPoints"), "blueprint.grammarPoints");
        if (grammarPoints.isEmpty()) {
            throw new LessonContentValidationException("blueprint.grammarPoints must not be empty");
        }
        for (int i = 0; i < grammarPoints.size(); i++) {
            var point = requireObject(grammarPoints.get(i), "blueprint.grammarPoints[" + i + "]");
            requireText(point.get("name"), "blueprint.grammarPoints[" + i + "].name");
            requireText(point.get("pattern"), "blueprint.grammarPoints[" + i + "].pattern");
        }
        requireText(artifact.get("lessonTone"), "blueprint.lessonTone");
        requireText(artifact.get("lessonGoal"), "blueprint.lessonGoal");
    }

    public void validateGrammarArtifact(JsonNode artifact) {
        requireObject(artifact, "grammarArtifact");
        var sections = requireArray(artifact.get("grammarSections"), "grammarArtifact.grammarSections");
        if (sections.isEmpty()) {
            throw new LessonContentValidationException("grammarArtifact.grammarSections must not be empty");
        }
        for (int i = 0; i < sections.size(); i++) {
            validateGrammarSection(requireObject(sections.get(i), "grammarArtifact.grammarSections[" + i + "]"),
                    "grammarArtifact.grammarSections[" + i + "]");
        }
    }

    public void validateVocabularyPracticeArtifact(JsonNode artifact, JsonNode blueprint) {
        requireObject(artifact, "vocabularyPracticeArtifact");
        var knownWords = wordsFromBlueprint(blueprint);
        var sections = requireArray(artifact.get("sections"), "vocabularyPracticeArtifact.sections");
        if (sections.isEmpty()) {
            throw new LessonContentValidationException("vocabularyPracticeArtifact.sections must not be empty");
        }
        for (int i = 0; i < sections.size(); i++) {
            var section = requireObject(sections.get(i), "vocabularyPracticeArtifact.sections[" + i + "]");
            requireExactText(section.get("type"), "vocabularyPracticeArtifact.sections[" + i + "].type", "word_study");
            var word = requireText(section.get("word"), "vocabularyPracticeArtifact.sections[" + i + "].word");
            if (!knownWords.contains(word)) {
                throw new LessonContentValidationException(
                        "vocabularyPracticeArtifact.sections[" + i + "].word must exist in blueprint vocabulary");
            }
            requireText(section.get("pinyin"), "vocabularyPracticeArtifact.sections[" + i + "].pinyin");
            requireText(section.get("translation"), "vocabularyPracticeArtifact.sections[" + i + "].translation");
            requireText(section.get("vocabularyStatus"), "vocabularyPracticeArtifact.sections[" + i + "].vocabularyStatus");
            requireArray(section.get("sentences"), "vocabularyPracticeArtifact.sections[" + i + "].sentences");
        }
    }

    public void validateWordGameArtifact(JsonNode artifact, JsonNode blueprint) {
        requireObject(artifact, "wordGameArtifact");
        var section = requireObject(artifact.get("section"), "wordGameArtifact.section");
        requireExactText(section.get("type"), "wordGameArtifact.section.type", "word_game");
        requireText(section.get("title"), "wordGameArtifact.section.title");
        requireText(section.get("instructions"), "wordGameArtifact.section.instructions");
        validateGameRounds(requireArray(section.get("rounds"), "wordGameArtifact.section.rounds"), wordsFromBlueprint(blueprint));
    }

    public void validateGrammarSection(JsonNode section, String path) {
        requireExactText(section.get("type"), path + ".type", "grammar");
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
            validateExamples(requireArray(point.get("examples"), path + ".points[" + i + "].examples"),
                    path + ".points[" + i + "].examples");
            validateExercises(requireArray(point.get("exercises"), path + ".points[" + i + "].exercises"),
                    path + ".points[" + i + "].exercises");
        }
    }

    private void validateExamples(JsonNode examples, String path) {
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

    private void validateExercises(JsonNode exercises, String path) {
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

    private void validateWordArray(JsonNode words, String path) {
        for (int i = 0; i < words.size(); i++) {
            var word = requireObject(words.get(i), path + "[" + i + "]");
            requireText(word.get("word"), path + "[" + i + "].word");
            requireText(word.get("pinyin"), path + "[" + i + "].pinyin");
            requireText(word.get("translation"), path + "[" + i + "].translation");
        }
    }

    private void validateGameRounds(JsonNode rounds, Set<String> knownWords) {
        if (rounds.isEmpty()) {
            throw new LessonContentValidationException("wordGameArtifact.section.rounds must not be empty");
        }
        for (int i = 0; i < rounds.size(); i++) {
            var round = requireObject(rounds.get(i), "wordGameArtifact.section.rounds[" + i + "]");
            requireText(round.get("prompt"), "wordGameArtifact.section.rounds[" + i + "].prompt");
            var answerWord = requireText(round.get("answerWord"), "wordGameArtifact.section.rounds[" + i + "].answerWord");
            if (!knownWords.contains(answerWord)) {
                throw new LessonContentValidationException(
                        "wordGameArtifact.section.rounds[" + i + "].answerWord must exist in blueprint vocabulary");
            }
        }
    }

    private Set<String> wordsFromBlueprint(JsonNode blueprint) {
        var words = new LinkedHashSet<String>();
        appendWords(words, requireArray(blueprint.get("newWords"), "blueprint.newWords"));
        appendWords(words, requireArray(blueprint.get("reviewWords"), "blueprint.reviewWords"));
        return Set.copyOf(words);
    }

    private void appendWords(Set<String> words, JsonNode items) {
        for (var item : items) {
            words.add(requireText(item.get("word"), "blueprint vocabulary word"));
        }
    }

    private void validateOptionalText(JsonNode node, String path) {
        if (node != null && !node.isNull()) {
            requireText(node, path);
        }
    }

    private void validateOptionalTextArray(JsonNode node, String path) {
        if (node == null || node.isNull()) {
            return;
        }
        var array = requireArray(node, path);
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

    private static void requireExactText(JsonNode node, String fieldName, String expected) {
        var value = requireText(node, fieldName);
        if (!expected.equals(value)) {
            throw new LessonContentValidationException(fieldName + " must be \"" + expected + "\"");
        }
    }
}
