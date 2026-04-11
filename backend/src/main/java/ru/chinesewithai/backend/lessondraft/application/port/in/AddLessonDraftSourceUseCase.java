package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.AddLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface AddLessonDraftSourceUseCase {
    LessonDraftView addSource(AddLessonDraftSourceCommand command);
}
