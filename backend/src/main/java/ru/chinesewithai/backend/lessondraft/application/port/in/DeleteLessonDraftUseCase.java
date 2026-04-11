package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.DeleteLessonDraftCommand;

public interface DeleteLessonDraftUseCase {
    void deleteDraft(DeleteLessonDraftCommand command);
}
