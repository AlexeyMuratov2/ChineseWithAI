package ru.chinesewithai.backend.lessondraft.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lessondraft.domain.model.LanguageTag;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceId;

class LessonDraftDomainTest {

    @Test
    void createNewUsesDefaultLanguages() {
        var draft = LessonDraft.createNew(
                UUID.randomUUID(), "My draft", "description", "instructions", null, null, Instant.now());

        assertThat(draft.explanationLanguage().value()).isEqualTo("zh");
        assertThat(draft.translationLanguage().value()).isEqualTo("en");
        assertThat(draft.sourceCount()).isZero();
    }

    @Test
    void removeSourceReindexesPositions() {
        var now = Instant.now();
        var ownerId = UUID.randomUUID();
        var draft = LessonDraft.createNew(
                        ownerId, "My draft", null, null, LanguageTag.of("zh"), LanguageTag.of("en"), now)
                .addTextSource("text-1", now.plusSeconds(10))
                .addTextSource("text-2", now.plusSeconds(20))
                .addDocumentSource(UUID.randomUUID(), "file.pdf", now.plusSeconds(30));

        var toRemove = draft.sources().get(1).id();
        var updated = draft.removeSource(toRemove, now.plusSeconds(40));

        assertThat(updated.sources()).hasSize(2);
        assertThat(updated.sources().get(0).position()).isEqualTo(0);
        assertThat(updated.sources().get(1).position()).isEqualTo(1);
        assertThat(updated.sources().stream().map(source -> source.id())).doesNotContain(toRemove);
    }

    @Test
    void reorderRequiresExactSourceSet() {
        var now = Instant.now();
        var ownerId = UUID.randomUUID();
        var draft = LessonDraft.createNew(ownerId, "Draft", null, null, null, null, now)
                .addTextSource("text-1", now.plusSeconds(1))
                .addTextSource("text-2", now.plusSeconds(2));

        var unknownId = LessonDraftSourceId.newId();

        assertThatThrownBy(() -> draft.reorderSources(List.of(draft.sources().get(0).id(), unknownId), now.plusSeconds(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("match current source ids");
    }

    @Test
    void textSourceRejectsBlankText() {
        var now = Instant.now();
        var ownerId = UUID.randomUUID();
        var draft = LessonDraft.createNew(ownerId, "Draft", null, null, null, null, now);

        assertThatThrownBy(() -> draft.addTextSource("   ", now.plusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("textContent");
    }
}
