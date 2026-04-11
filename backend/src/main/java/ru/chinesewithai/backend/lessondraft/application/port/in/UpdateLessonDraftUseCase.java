package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.UpdateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface UpdateLessonDraftUseCase {
    LessonDraftView updateDraft(UpdateLessonDraftCommand command);
}
