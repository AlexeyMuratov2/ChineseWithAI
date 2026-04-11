package ru.chinesewithai.backend.lessondraft.application.port.in;

import ru.chinesewithai.backend.lessondraft.application.command.GetLessonDraftQuery;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface GetLessonDraftUseCase {
    LessonDraftView getDraft(GetLessonDraftQuery query);
}
