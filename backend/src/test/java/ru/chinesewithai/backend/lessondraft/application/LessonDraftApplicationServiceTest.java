package ru.chinesewithai.backend.lessondraft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lessondraft.application.command.AddLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.command.CreateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.command.RemoveLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.command.ReorderLessonDraftSourcesCommand;
import ru.chinesewithai.backend.lessondraft.application.exception.InvalidSourcePayloadException;
import ru.chinesewithai.backend.lessondraft.application.exception.SourceNotFoundException;
import ru.chinesewithai.backend.lessondraft.application.exception.SourceOrderMismatchException;
import ru.chinesewithai.backend.lessondraft.application.port.out.LessonDraftRepository;
import ru.chinesewithai.backend.lessondraft.application.service.ExtractedLessonDraftDocumentSource;
import ru.chinesewithai.backend.lessondraft.application.service.LessonDraftDocumentTextExtractor;
import ru.chinesewithai.backend.lessondraft.application.service.LessonDraftApplicationService;
import ru.chinesewithai.backend.lessondraft.domain.model.LanguageTag;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceType;

class LessonDraftApplicationServiceTest {

    private final LessonDraftRepository lessonDraftRepository = mock(LessonDraftRepository.class);
    private final LessonDraftDocumentTextExtractor documentTextExtractor = mock(LessonDraftDocumentTextExtractor.class);
    private final LessonDraftApplicationService service =
            new LessonDraftApplicationService(lessonDraftRepository, documentTextExtractor);

    @Test
    void createDraftUsesDefaultLanguages() {
        when(lessonDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var view = service.createDraft(new CreateLessonDraftCommand("My draft", null, null, null, null));

        assertThat(view.explanationLanguage()).isEqualTo("zh");
        assertThat(view.translationLanguage()).isEqualTo("en");
    }

    @Test
    void addDocumentSourceRejectsTextPayload() {
        var draft = newDraft();
        when(lessonDraftRepository.findById(draft.id())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.addSource(new AddLessonDraftSourceCommand(
                        draft.id().value(),
                        LessonDraftSourceType.DOCUMENT_FILE,
                        "should-not-be-here",
                        UUID.randomUUID(),
                        "doc.pdf")))
                .isInstanceOf(InvalidSourcePayloadException.class);
    }

    @Test
    void addDocumentSourceStoresExtractedText() {
        var draft = newDraft();
        var documentFileId = UUID.randomUUID();
        when(lessonDraftRepository.findById(draft.id())).thenReturn(Optional.of(draft));
        when(documentTextExtractor.extract(documentFileId, "source.txt"))
                .thenReturn(new ExtractedLessonDraftDocumentSource("extracted text", "source.txt"));
        when(lessonDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var view = service.addSource(new AddLessonDraftSourceCommand(
                draft.id().value(), LessonDraftSourceType.DOCUMENT_FILE, null, documentFileId, "source.txt"));

        assertThat(view.sources()).singleElement().satisfies(source -> {
            assertThat(source.type()).isEqualTo("DOCUMENT_FILE");
            assertThat(source.textContent()).isEqualTo("extracted text");
            assertThat(source.documentOriginalFileName()).isEqualTo("source.txt");
        });
    }

    @Test
    void removeSourceThrowsWhenSourceMissing() {
        var draft = newDraft();
        when(lessonDraftRepository.findById(draft.id())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.removeSource(
                        new RemoveLessonDraftSourceCommand(draft.id().value(), UUID.randomUUID())))
                .isInstanceOf(SourceNotFoundException.class);
    }

    @Test
    void reorderSourcesThrowsForMismatchedIds() {
        var draft = newDraft().addTextSource("note", Instant.now().plusSeconds(30));
        when(lessonDraftRepository.findById(draft.id())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.reorderSources(new ReorderLessonDraftSourcesCommand(
                        draft.id().value(), List.of(UUID.randomUUID()))))
                .isInstanceOf(SourceOrderMismatchException.class);
    }

    private static LessonDraft newDraft() {
        return LessonDraft.createNew(
                "Draft", "description", "instructions", LanguageTag.of("zh"), LanguageTag.of("en"), Instant.now());
    }
}
