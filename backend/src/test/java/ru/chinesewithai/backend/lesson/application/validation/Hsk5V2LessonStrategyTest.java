package ru.chinesewithai.backend.lesson.application.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

class Hsk5V2LessonStrategyTest {

    private final Hsk5V2LessonStrategy strategy = new Hsk5V2LessonStrategy();
    private final Hsk5V1LessonStrategy legacyStrategy = new Hsk5V1LessonStrategy();

    @Test
    void acceptsMultipleTextAndDocumentSources() {
        assertThatCode(() -> strategy.validateDraftForGeneration(draft(List.of(
                        textSource("第一段", 0),
                        textSource("第二段", 1),
                        documentSource(2)))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyDraft() {
        assertThatThrownBy(() -> strategy.validateDraftForGeneration(draft(List.of())))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("between 1 and");
    }

    @Test
    void legacyHsk5V1StillRejectsMultipleSources() {
        assertThatThrownBy(() -> legacyStrategy.validateDraftForGeneration(draft(List.of(
                        textSource("第一段", 0),
                        textSource("第二段", 1)))))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("exactly one draft source");
    }

    private LessonDraftView draft(List<LessonDraftSourceView> sources) {
        return new LessonDraftView(
                UUID.randomUUID(),
                "Draft",
                null,
                null,
                "ru",
                "ru",
                sources,
                Instant.now(),
                Instant.now(),
                0L);
    }

    private LessonDraftSourceView textSource(String text, int position) {
        return new LessonDraftSourceView(
                UUID.randomUUID(), "TEXT_NOTE", position, text, null, null, Instant.now(), Instant.now());
    }

    private LessonDraftSourceView documentSource(int position) {
        return new LessonDraftSourceView(
                UUID.randomUUID(),
                "DOCUMENT_FILE",
                position,
                null,
                UUID.randomUUID(),
                "page.png",
                Instant.now(),
                Instant.now());
    }
}
