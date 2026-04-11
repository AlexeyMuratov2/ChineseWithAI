package ru.chinesewithai.backend.lessondraft.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.lessondraft.application.command.AddLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.command.CreateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.command.DeleteLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.command.GetLessonDraftQuery;
import ru.chinesewithai.backend.lessondraft.application.command.ListMyLessonDraftsQuery;
import ru.chinesewithai.backend.lessondraft.application.command.RemoveLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.command.ReorderLessonDraftSourcesCommand;
import ru.chinesewithai.backend.lessondraft.application.command.UpdateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.exception.InvalidSourcePayloadException;
import ru.chinesewithai.backend.lessondraft.application.exception.LessonDraftNotFoundException;
import ru.chinesewithai.backend.lessondraft.application.exception.SourceNotFoundException;
import ru.chinesewithai.backend.lessondraft.application.exception.SourceOrderMismatchException;
import ru.chinesewithai.backend.lessondraft.application.port.in.AddLessonDraftSourceUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.CreateLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.DeleteLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.GetLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.ListMyLessonDraftsUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.RemoveLessonDraftSourceUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.ReorderLessonDraftSourcesUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.UpdateLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.out.CurrentUserIdProvider;
import ru.chinesewithai.backend.lessondraft.application.port.out.LessonDraftRepository;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftPageView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSummaryView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.lessondraft.domain.model.LanguageTag;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftId;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceId;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceType;

@Service
public class LessonDraftApplicationService
        implements CreateLessonDraftUseCase,
                UpdateLessonDraftUseCase,
                GetLessonDraftUseCase,
                ListMyLessonDraftsUseCase,
                AddLessonDraftSourceUseCase,
                RemoveLessonDraftSourceUseCase,
                ReorderLessonDraftSourcesUseCase,
                DeleteLessonDraftUseCase {

    private final LessonDraftRepository lessonDraftRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public LessonDraftApplicationService(
            LessonDraftRepository lessonDraftRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.lessonDraftRepository = lessonDraftRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Override
    @Transactional
    public LessonDraftView createDraft(CreateLessonDraftCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var now = Instant.now();

        var draft = LessonDraft.createNew(
                ownerId,
                command.title(),
                command.description(),
                command.userInstructions(),
                toLanguageTagOrNull(command.explanationLanguage()),
                toLanguageTagOrNull(command.translationLanguage()),
                now);

        return toView(lessonDraftRepository.save(draft));
    }

    @Override
    @Transactional
    public LessonDraftView updateDraft(UpdateLessonDraftCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var draft = requireOwnedDraft(command.draftId(), ownerId);
        var now = Instant.now();

        var updated = draft.updateMetadata(
                command.title(),
                command.description(),
                command.userInstructions(),
                toLanguageTagOrNull(command.explanationLanguage()),
                toLanguageTagOrNull(command.translationLanguage()),
                now);

        return toView(lessonDraftRepository.save(updated));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDraftView getDraft(GetLessonDraftQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        return toView(requireOwnedDraft(query.draftId(), ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDraftPageView listMyDrafts(ListMyLessonDraftsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var page = lessonDraftRepository.findPageByOwnerId(ownerId, query.page(), query.size());

        var items = page.items().stream()
                .map(item -> new LessonDraftSummaryView(
                        item.id(),
                        item.title(),
                        item.explanationLanguage(),
                        item.translationLanguage(),
                        item.sourceCount(),
                        item.createdAt(),
                        item.updatedAt(),
                        item.version()))
                .toList();

        return new LessonDraftPageView(
                items, query.page(), query.size(), page.totalElements(), page.totalPages(), page.hasNext());
    }

    @Override
    @Transactional
    public LessonDraftView addSource(AddLessonDraftSourceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var draft = requireOwnedDraft(command.draftId(), ownerId);
        var now = Instant.now();

        var updated = switch (command.type()) {
            case TEXT_NOTE -> {
                validateTextNotePayload(command);
                yield draft.addTextSource(command.textContent(), now);
            }
            case DOCUMENT_FILE -> {
                validateDocumentPayload(command);
                yield draft.addDocumentSource(command.documentFileId(), command.documentOriginalFileName(), now);
            }
        };

        return toView(lessonDraftRepository.save(updated));
    }

    @Override
    @Transactional
    public LessonDraftView removeSource(RemoveLessonDraftSourceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var draft = requireOwnedDraft(command.draftId(), ownerId);
        var sourceId = new LessonDraftSourceId(command.sourceId());
        if (!draft.containsSource(sourceId)) {
            throw new SourceNotFoundException(command.sourceId());
        }

        var updated = draft.removeSource(sourceId, Instant.now());
        return toView(lessonDraftRepository.save(updated));
    }

    @Override
    @Transactional
    public LessonDraftView reorderSources(ReorderLessonDraftSourcesCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var draft = requireOwnedDraft(command.draftId(), ownerId);
        var orderedIds = command.orderedSourceIds().stream().map(LessonDraftSourceId::new).toList();

        try {
            var updated = draft.reorderSources(orderedIds, Instant.now());
            return toView(lessonDraftRepository.save(updated));
        } catch (IllegalArgumentException ex) {
            throw new SourceOrderMismatchException(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteDraft(DeleteLessonDraftCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var ownerId = currentUserIdProvider.getCurrentUserId();
        var draft = requireOwnedDraft(command.draftId(), ownerId);
        lessonDraftRepository.delete(draft);
    }

    private LessonDraft requireOwnedDraft(UUID draftId, UUID ownerId) {
        return lessonDraftRepository
                .findByIdAndOwnerId(new LessonDraftId(draftId), ownerId)
                .orElseThrow(() -> new LessonDraftNotFoundException(draftId));
    }

    private static LanguageTag toLanguageTagOrNull(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        return LanguageTag.of(rawValue);
    }

    private static void validateTextNotePayload(AddLessonDraftSourceCommand command) {
        if (command.textContent() == null || command.textContent().isBlank()) {
            throw new InvalidSourcePayloadException("textContent must be provided for TEXT_NOTE");
        }
        if (command.documentFileId() != null || command.documentOriginalFileName() != null) {
            throw new InvalidSourcePayloadException("document payload must be null for TEXT_NOTE");
        }
    }

    private static void validateDocumentPayload(AddLessonDraftSourceCommand command) {
        if (command.documentFileId() == null) {
            throw new InvalidSourcePayloadException("documentFileId must be provided for DOCUMENT_FILE");
        }
        if (command.textContent() != null) {
            throw new InvalidSourcePayloadException("textContent must be null for DOCUMENT_FILE");
        }
    }

    private static LessonDraftView toView(LessonDraft draft) {
        var sourceViews = draft.sources().stream()
                .map(source -> new LessonDraftSourceView(
                        source.id().value(),
                        source.type(),
                        source.position(),
                        source.textContent(),
                        source.documentFileId(),
                        source.documentOriginalFileName(),
                        source.createdAt(),
                        source.updatedAt()))
                .toList();

        return new LessonDraftView(
                draft.id().value(),
                draft.ownerId(),
                draft.title(),
                draft.description(),
                draft.userInstructions(),
                draft.explanationLanguage().value(),
                draft.translationLanguage().value(),
                sourceViews,
                draft.createdAt(),
                draft.updatedAt(),
                draft.version());
    }
}
