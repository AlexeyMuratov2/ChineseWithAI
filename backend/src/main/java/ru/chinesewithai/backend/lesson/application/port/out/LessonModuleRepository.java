package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.Optional;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

public interface LessonModuleRepository {
    Optional<LessonModule> findByModuleKey(String moduleKey);
}
