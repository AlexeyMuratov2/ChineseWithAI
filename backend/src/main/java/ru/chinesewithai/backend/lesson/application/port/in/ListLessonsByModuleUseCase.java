package ru.chinesewithai.backend.lesson.application.port.in;

import java.util.List;
import ru.chinesewithai.backend.lesson.application.view.LessonView;

public interface ListLessonsByModuleUseCase {
    List<LessonView> listByModuleKey(String moduleKey);
}
