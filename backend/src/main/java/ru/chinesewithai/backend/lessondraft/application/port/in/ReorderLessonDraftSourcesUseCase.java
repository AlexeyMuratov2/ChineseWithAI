package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.ReorderLessonDraftSourcesCommand;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface ReorderLessonDraftSourcesUseCase {
    LessonDraftView reorderSources(ReorderLessonDraftSourcesCommand command);
}
