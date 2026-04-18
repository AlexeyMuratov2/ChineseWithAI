package ru.chinesewithai.backend.lesson.application.port.in;

import java.util.List;
import ru.chinesewithai.backend.lesson.application.view.LessonModuleSummaryView;

public interface ListLessonModulesUseCase {
    List<LessonModuleSummaryView> listAll();
}
