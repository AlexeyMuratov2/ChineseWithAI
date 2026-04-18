package ru.chinesewithai.backend.lesson.application.port.in;

import ru.chinesewithai.backend.lesson.application.command.GenerateLessonFromDraftCommand;
import ru.chinesewithai.backend.lesson.application.view.LessonView;

public interface GenerateLessonFromDraftUseCase {
    LessonView generateFromDraft(GenerateLessonFromDraftCommand command);
}
