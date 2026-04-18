package ru.chinesewithai.backend.lesson.application.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

class Hsk5V1LessonStrategyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Hsk5V1LessonStrategy strategy = new Hsk5V1LessonStrategy();
    private final LessonModule module = new LessonModule(
            "hsk5_v1",
            "HSK 5 v1",
            "prompt",
            1,
            true,
            "lesson-generator:hsk5_v1",
            null,
            Instant.now(),
            Instant.now());

    @Test
    void acceptsValidPayload() {
        assertThatCode(() -> strategy.validateLesson(validLesson(), module)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidDraftShape() {
        assertThatThrownBy(() -> strategy.validateDraftForGeneration(draft(List.of(
                        textSource("text one", 0),
                        textSource("text two", 1)))))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("exactly one TEXT_NOTE");

        assertThatThrownBy(() -> strategy.validateDraftForGeneration(draft(List.of(documentSource()))))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("only TEXT_NOTE");
    }

    @Test
    void rejectsUnknownSectionTypeBecauseFrontendCannotRenderIt() {
        var lesson = (ObjectNode) validLesson();
        ((ObjectNode) ((ArrayNode) lesson.get("sections")).get(0)).put("type", "mystery_block");

        assertThatThrownBy(() -> strategy.validateLesson(lesson, module))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("unsupported hsk5_v1 block type");
    }

    @Test
    void rejectsMissingRenderableBlockFields() {
        var lesson = (ObjectNode) validLesson();
        var sections = (ArrayNode) lesson.get("sections");
        var textSection = (ObjectNode) sections.get(3);
        textSection.remove("readingPrompt");

        assertThatThrownBy(() -> strategy.validateLesson(lesson, module))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("sections[3].readingPrompt must be a string");
    }

    @Test
    void rejectsMissingReviewWordsArray() {
        var lesson = (ObjectNode) validLesson();
        lesson.remove("reviewWords");

        assertThatThrownBy(() -> strategy.validateLesson(lesson, module))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("reviewWords must be an array");
    }

    @Test
    void allowsSemanticallyImperfectButRenderablePayload() {
        var lesson = (ObjectNode) validLesson();
        var sections = (ArrayNode) lesson.get("sections");
        var firstWordStudy = (ObjectNode) sections.get(0);
        firstWordStudy.put("vocabularyStatus", "review");
        ((ArrayNode) firstWordStudy.get("sentences")).remove(1);
        ((ObjectNode) sections.get(3)).put("text", "different source text");
        ((ObjectNode) sections.get(3)).put("translation", "optional fallback translation");
        ((ArrayNode) ((ObjectNode) sections.get(4)).get("rounds")).remove(1);
        sections.insert(1, firstWordStudy.deepCopy());
        removeFirstSectionOfType(lesson, "conversation");

        assertThatCode(() -> strategy.validateLesson(lesson, module)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAndCanonicalizesCommonModelFieldAliases() {
        var lesson = (ObjectNode) validLesson();
        var sections = (ArrayNode) lesson.get("sections");
        var firstWordStudy = (ObjectNode) sections.get(0);
        firstWordStudy.set("exampleSentences", firstWordStudy.remove("sentences"));
        var firstRound = (ObjectNode) ((ArrayNode) ((ObjectNode) sections.get(4)).get("rounds")).get(0);
        firstRound.set("expectedWord", firstRound.remove("answerWord"));
        firstRound.remove("vocabularyStatus");

        assertThatCode(() -> strategy.validateLesson(lesson, module)).doesNotThrowAnyException();
        assertThat(firstWordStudy.has("sentences")).isTrue();
        assertThat(firstWordStudy.has("exampleSentences")).isFalse();
        assertThat(firstRound.path("answerWord").asText()).isEqualTo("jihui");
        assertThat(firstRound.has("expectedWord")).isFalse();
    }

    private JsonNode validLesson() {
        return objectMapper.valueToTree(Map.of(
                "schemaVersion", 1,
                "moduleKey", "hsk5_v1",
                "title", "A real HSK5 lesson",
                "studyLanguage", "zh",
                "explanationLanguage", "ru",
                "translationLanguage", "ru",
                "newWords", List.of(Map.of("word", "jihui", "pinyin", "ji hui", "translation", "opportunity")),
                "reviewWords", List.of(Map.of("word", "yingxiang", "pinyin", "ying xiang", "translation", "influence")),
                "sections", List.of(
                        wordStudy("jihui", "ji hui", "opportunity", "new"),
                        wordStudy("yingxiang", "ying xiang", "influence", "review"),
                        Map.of(
                                "type", "conversation",
                                "title", "Talk",
                                "mode", "free_talk",
                                "prompt", "Use today's words.",
                                "followUpPrompts", List.of("Why?")),
                        Map.of(
                                "type", "text",
                                "title", "Text",
                                "text", "Short source text.",
                                "readingPrompt", "Read it aloud.",
                                "discussionPrompts", List.of("What is the main idea?")),
                        Map.of(
                                "type", "word_game",
                                "title", "Game",
                                "instructions", "Guess the word.",
                                "rounds", List.of(
                                        Map.of("prompt", "chance", "answerWord", "jihui", "vocabularyStatus", "new"),
                                        Map.of("prompt", "effect", "answerWord", "yingxiang", "vocabularyStatus", "review"))))));
    }

    private Map<String, Object> wordStudy(String word, String pinyin, String translation, String vocabularyStatus) {
        return Map.of(
                "type", "word_study",
                "title", "Word: " + word,
                "word", word,
                "pinyin", pinyin,
                "translation", translation,
                "vocabularyStatus", vocabularyStatus,
                "sentences", List.of(
                        Map.of("sentence", "Example one with " + word, "translation", "Example one."),
                        Map.of("sentence", "Example two without semantic checks", "translation", "Example two.")));
    }

    private void removeFirstSectionOfType(ObjectNode lesson, String type) {
        var sections = (ArrayNode) lesson.get("sections");
        for (int i = 0; i < sections.size(); i++) {
            if (type.equals(sections.get(i).path("type").asText())) {
                sections.remove(i);
                return;
            }
        }
    }

    private LessonDraftView draft(List<LessonDraftSourceView> sources) {
        return new LessonDraftView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Draft",
                null,
                null,
                "zh",
                "en",
                sources,
                Instant.now(),
                Instant.now(),
                0L);
    }

    private LessonDraftSourceView textSource(String text, int position) {
        return new LessonDraftSourceView(
                UUID.randomUUID(), "TEXT_NOTE", position, text, null, null, Instant.now(), Instant.now());
    }

    private LessonDraftSourceView documentSource() {
        return new LessonDraftSourceView(
                UUID.randomUUID(), "DOCUMENT_FILE", 0, null, UUID.randomUUID(), "source.pdf", Instant.now(), Instant.now());
    }
}
