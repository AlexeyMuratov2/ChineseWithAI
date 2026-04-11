package ru.chinesewithai.backend.lessondraft.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftId;

public interface LessonDraftRepository {
    LessonDraft save(LessonDraft draft);

    Optional<LessonDraft> findByIdAndOwnerId(LessonDraftId draftId, UUID ownerId);

    LessonDraftPage findPageByOwnerId(UUID ownerId, int page, int size);

    void delete(LessonDraft draft);
}
