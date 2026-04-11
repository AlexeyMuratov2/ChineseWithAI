package ru.chinesewithai.backend.lessondraft.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.lessondraft.api.dto.AddLessonDraftSourceRequest;
import ru.chinesewithai.backend.lessondraft.api.dto.CreateLessonDraftRequest;
import ru.chinesewithai.backend.lessondraft.api.dto.LessonDraftPageResponse;
import ru.chinesewithai.backend.lessondraft.api.dto.LessonDraftResponse;
import ru.chinesewithai.backend.lessondraft.api.dto.LessonDraftSourceResponse;
import ru.chinesewithai.backend.lessondraft.api.dto.LessonDraftSummaryResponse;
import ru.chinesewithai.backend.lessondraft.api.dto.ReorderLessonDraftSourcesRequest;
import ru.chinesewithai.backend.lessondraft.api.dto.UpdateLessonDraftRequest;
import ru.chinesewithai.backend.lessondraft.application.command.AddLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.command.CreateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.command.DeleteLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.command.GetLessonDraftQuery;
import ru.chinesewithai.backend.lessondraft.application.command.ListMyLessonDraftsQuery;
import ru.chinesewithai.backend.lessondraft.application.command.RemoveLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.command.ReorderLessonDraftSourcesCommand;
import ru.chinesewithai.backend.lessondraft.application.command.UpdateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.port.in.AddLessonDraftSourceUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.CreateLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.DeleteLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.GetLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.ListMyLessonDraftsUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.RemoveLessonDraftSourceUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.ReorderLessonDraftSourcesUseCase;
import ru.chinesewithai.backend.lessondraft.application.port.in.UpdateLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftPageView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

@RestController
@RequestMapping("/api/v1/lesson-drafts")
public class LessonDraftController {

    private final CreateLessonDraftUseCase createLessonDraftUseCase;
    private final UpdateLessonDraftUseCase updateLessonDraftUseCase;
    private final GetLessonDraftUseCase getLessonDraftUseCase;
    private final ListMyLessonDraftsUseCase listMyLessonDraftsUseCase;
    private final AddLessonDraftSourceUseCase addLessonDraftSourceUseCase;
    private final RemoveLessonDraftSourceUseCase removeLessonDraftSourceUseCase;
    private final ReorderLessonDraftSourcesUseCase reorderLessonDraftSourcesUseCase;
    private final DeleteLessonDraftUseCase deleteLessonDraftUseCase;

    public LessonDraftController(
            CreateLessonDraftUseCase createLessonDraftUseCase,
            UpdateLessonDraftUseCase updateLessonDraftUseCase,
            GetLessonDraftUseCase getLessonDraftUseCase,
            ListMyLessonDraftsUseCase listMyLessonDraftsUseCase,
            AddLessonDraftSourceUseCase addLessonDraftSourceUseCase,
            RemoveLessonDraftSourceUseCase removeLessonDraftSourceUseCase,
            ReorderLessonDraftSourcesUseCase reorderLessonDraftSourcesUseCase,
            DeleteLessonDraftUseCase deleteLessonDraftUseCase) {
        this.createLessonDraftUseCase = createLessonDraftUseCase;
        this.updateLessonDraftUseCase = updateLessonDraftUseCase;
        this.getLessonDraftUseCase = getLessonDraftUseCase;
        this.listMyLessonDraftsUseCase = listMyLessonDraftsUseCase;
        this.addLessonDraftSourceUseCase = addLessonDraftSourceUseCase;
        this.removeLessonDraftSourceUseCase = removeLessonDraftSourceUseCase;
        this.reorderLessonDraftSourcesUseCase = reorderLessonDraftSourcesUseCase;
        this.deleteLessonDraftUseCase = deleteLessonDraftUseCase;
    }

    @PostMapping
    public ResponseEntity<LessonDraftResponse> createDraft(@Valid @RequestBody CreateLessonDraftRequest request) {
        var view = createLessonDraftUseCase.createDraft(new CreateLessonDraftCommand(
                request.title(),
                request.description(),
                request.userInstructions(),
                request.explanationLanguage(),
                request.translationLanguage()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(view));
    }

    @PutMapping("/{draftId}")
    public LessonDraftResponse updateDraft(
            @PathVariable UUID draftId, @Valid @RequestBody UpdateLessonDraftRequest request) {
        var view = updateLessonDraftUseCase.updateDraft(new UpdateLessonDraftCommand(
                draftId,
                request.title(),
                request.description(),
                request.userInstructions(),
                request.explanationLanguage(),
                request.translationLanguage()));
        return toResponse(view);
    }

    @GetMapping("/{draftId}")
    public LessonDraftResponse getDraft(@PathVariable UUID draftId) {
        var view = getLessonDraftUseCase.getDraft(new GetLessonDraftQuery(draftId));
        return toResponse(view);
    }

    @GetMapping
    public LessonDraftPageResponse listDrafts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        var view = listMyLessonDraftsUseCase.listMyDrafts(new ListMyLessonDraftsQuery(page, size));
        return toPageResponse(view);
    }

    @PostMapping("/{draftId}/sources")
    public LessonDraftResponse addSource(
            @PathVariable UUID draftId, @Valid @RequestBody AddLessonDraftSourceRequest request) {
        var view = addLessonDraftSourceUseCase.addSource(new AddLessonDraftSourceCommand(
                draftId,
                request.type(),
                request.textContent(),
                request.documentFileId(),
                request.documentOriginalFileName()));
        return toResponse(view);
    }

    @DeleteMapping("/{draftId}/sources/{sourceId}")
    public LessonDraftResponse removeSource(@PathVariable UUID draftId, @PathVariable UUID sourceId) {
        var view =
                removeLessonDraftSourceUseCase.removeSource(new RemoveLessonDraftSourceCommand(draftId, sourceId));
        return toResponse(view);
    }

    @PutMapping("/{draftId}/sources/reorder")
    public LessonDraftResponse reorderSources(
            @PathVariable UUID draftId, @Valid @RequestBody ReorderLessonDraftSourcesRequest request) {
        var view = reorderLessonDraftSourcesUseCase.reorderSources(
                new ReorderLessonDraftSourcesCommand(draftId, request.sourceIds()));
        return toResponse(view);
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID draftId) {
        deleteLessonDraftUseCase.deleteDraft(new DeleteLessonDraftCommand(draftId));
        return ResponseEntity.noContent().build();
    }

    private static LessonDraftResponse toResponse(LessonDraftView view) {
        var sources = view.sources().stream()
                .map(source -> new LessonDraftSourceResponse(
                        source.id(),
                        source.type(),
                        source.position(),
                        source.textContent(),
                        source.documentFileId(),
                        source.documentOriginalFileName(),
                        source.createdAt(),
                        source.updatedAt()))
                .toList();
        return new LessonDraftResponse(
                view.id(),
                view.ownerId(),
                view.title(),
                view.description(),
                view.userInstructions(),
                view.explanationLanguage(),
                view.translationLanguage(),
                sources,
                view.createdAt(),
                view.updatedAt(),
                view.version());
    }

    private static LessonDraftPageResponse toPageResponse(LessonDraftPageView view) {
        var items = view.items().stream()
                .map(item -> new LessonDraftSummaryResponse(
                        item.id(),
                        item.title(),
                        item.explanationLanguage(),
                        item.translationLanguage(),
                        item.sourceCount(),
                        item.createdAt(),
                        item.updatedAt(),
                        item.version()))
                .toList();
        return new LessonDraftPageResponse(
                items, view.page(), view.size(), view.totalElements(), view.totalPages(), view.hasNext());
    }
}
