package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.CreateLessonDraftCommand;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface CreateLessonDraftUseCase {
    LessonDraftView createDraft(CreateLessonDraftCommand command);
}
