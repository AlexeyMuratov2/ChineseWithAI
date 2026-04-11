package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.ListMyLessonDraftsQuery;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftPageView;

public interface ListMyLessonDraftsUseCase {
    LessonDraftPageView listMyDrafts(ListMyLessonDraftsQuery query);
}
