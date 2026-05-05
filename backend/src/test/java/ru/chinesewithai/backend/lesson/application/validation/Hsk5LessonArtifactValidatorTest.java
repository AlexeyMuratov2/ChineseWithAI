package ru.chinesewithai.backend.lesson.application.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;

class Hsk5LessonArtifactValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Hsk5LessonArtifactValidator validator = new Hsk5LessonArtifactValidator();

    @Test
    void validatesStageArtifacts() {
        var blueprint = blueprint("source text");
        assertThatCode(() -> validator.validateBlueprint(blueprint, "source text")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateGrammarArtifact(grammar())).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateVocabularyPracticeArtifact(vocabularyPractice(), blueprint))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateWordGameArtifact(wordGame("机会"), blueprint)).doesNotThrowAnyException();
    }

    @Test
    void rejectsArtifactsThatBreakStageContracts() {
        assertThatThrownBy(() -> validator.validateBlueprint(blueprint("changed"), "source text"))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("must match the draft text exactly");

        assertThatThrownBy(() -> validator.validateGrammarArtifact(objectMapper.valueToTree(Map.of("grammarSections", List.of()))))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("must not be empty");

        assertThatThrownBy(() -> validator.validateWordGameArtifact(wordGame("missing"), blueprint("source text")))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("must exist in blueprint vocabulary");
    }

    private com.fasterxml.jackson.databind.JsonNode blueprint(String readingText) {
        return objectMapper.valueToTree(Map.of(
                "title", "Lesson",
                "readingText", readingText,
                "newWords", List.of(word("机会")),
                "reviewWords", List.of(word("影响")),
                "grammarPoints", List.of(Map.of("name", "Although", "pattern", "虽然...,但是...")),
                "lessonTone", "warm",
                "lessonGoal", "practice"));
    }

    private com.fasterxml.jackson.databind.JsonNode grammar() {
        return objectMapper.valueToTree(Map.of("grammarSections", List.of(Map.of(
                "type", "grammar",
                "title", "Grammar",
                "points", List.of(Map.of(
                        "name", "Although",
                        "pattern", "虽然...,但是...",
                        "explanation", "Contrast ideas.",
                        "examples", List.of(Map.of("sentence", "虽然很难，但是我想试试。", "translation", "Although it is hard, I want to try.")),
                        "exercises", List.of(Map.of("prompt", "Make one sentence."))))))));
    }

    private com.fasterxml.jackson.databind.JsonNode vocabularyPractice() {
        return objectMapper.valueToTree(Map.of("sections", List.of(
                wordStudy("机会", "new"),
                wordStudy("影响", "review"))));
    }

    private com.fasterxml.jackson.databind.JsonNode wordGame(String answerWord) {
        return objectMapper.valueToTree(Map.of("section", Map.of(
                "type", "word_game",
                "title", "Game",
                "instructions", "Guess",
                "rounds", List.of(Map.of("prompt", "chance", "answerWord", answerWord)))));
    }

    private Map<String, Object> word(String value) {
        return Map.of("word", value, "pinyin", "pinyin", "translation", "translation");
    }

    private Map<String, Object> wordStudy(String value, String status) {
        return Map.of(
                "type", "word_study",
                "title", value,
                "word", value,
                "pinyin", "pinyin",
                "translation", "translation",
                "vocabularyStatus", status,
                "sentences", List.of(Map.of("sentence", "句子", "translation", "Sentence")));
    }
}
