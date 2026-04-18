package ru.chinesewithai.backend.lesson.application.port.in;

import ru.chinesewithai.backend.lesson.application.command.GetLessonQuery;
import ru.chinesewithai.backend.lesson.application.view.LessonView;

public interface GetLessonUseCase {
    LessonView getLesson(GetLessonQuery query);
}
