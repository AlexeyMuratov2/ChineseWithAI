package ru.chinesewithai.backend.lesson.application.port.in;

import ru.chinesewithai.backend.lesson.application.command.CreateLessonFromJsonCommand;
import ru.chinesewithai.backend.lesson.application.view.LessonView;

public interface CreateLessonFromJsonUseCase {
    LessonView createFromJson(CreateLessonFromJsonCommand command);
}
