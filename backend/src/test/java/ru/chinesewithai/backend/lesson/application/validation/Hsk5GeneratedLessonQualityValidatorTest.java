package ru.chinesewithai.backend.lesson.application.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;

class Hsk5GeneratedLessonQualityValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Hsk5GeneratedLessonQualityValidator validator = new Hsk5GeneratedLessonQualityValidator();

    @Test
    void acceptsLessonWithTextGrammarWordStudyAndKnownGameRounds() {
        assertThatCode(() -> validator.validate(validLesson("source text"), "source text"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingQualitySignals() {
        assertThatThrownBy(() -> validator.validate(validLesson("changed text"), "source text"))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("exact draft text");

        assertThatThrownBy(() -> validator.validate(lessonWithUnknownGameAnswer(), "source text"))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("must exist in newWords or reviewWords");
    }

    private com.fasterxml.jackson.databind.JsonNode lessonWithUnknownGameAnswer() {
        var lesson = (com.fasterxml.jackson.databind.node.ObjectNode) validLesson("source text");
        var sections = (com.fasterxml.jackson.databind.node.ArrayNode) lesson.get("sections");
        var game = (com.fasterxml.jackson.databind.node.ObjectNode) sections.get(sections.size() - 1);
        ((com.fasterxml.jackson.databind.node.ObjectNode) game.withArray("rounds").get(0)).put("answerWord", "未知");
        return lesson;
    }

    private com.fasterxml.jackson.databind.JsonNode validLesson(String text) {
        return objectMapper.valueToTree(Map.of(
                "newWords", List.of(word("机会")),
                "reviewWords", List.of(word("影响")),
                "sections", List.of(
                        wordStudy("机会", "new"),
                        wordStudy("影响", "review"),
                        Map.of(
                                "type", "grammar",
                                "title", "Grammar",
                                "points", List.of(Map.of("name", "Although"))),
                        Map.of("type", "text", "title", "Text", "text", text),
                        Map.of(
                                "type", "word_game",
                                "rounds", List.of(Map.of("answerWord", "机会"))))));
    }

    private Map<String, Object> word(String value) {
        return Map.of("word", value, "pinyin", "pinyin", "translation", "translation");
    }

    private Map<String, Object> wordStudy(String value, String status) {
        return Map.of("type", "word_study", "word", value, "vocabularyStatus", status);
    }
}
