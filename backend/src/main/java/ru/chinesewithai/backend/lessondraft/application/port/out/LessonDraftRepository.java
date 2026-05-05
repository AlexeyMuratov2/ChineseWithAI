package ru.chinesewithai.backend.lessondraft.application.port.out;

import java.util.Optional;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftId;

public interface LessonDraftRepository {
    LessonDraft save(LessonDraft draft);

    Optional<LessonDraft> findById(LessonDraftId draftId);

    LessonDraftPage findPage(int page, int size);

    void delete(LessonDraft draft);
}
