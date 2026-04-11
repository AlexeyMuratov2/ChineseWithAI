package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.RemoveLessonDraftSourceCommand;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface RemoveLessonDraftSourceUseCase {
    LessonDraftView removeSource(RemoveLessonDraftSourceCommand command);
}
