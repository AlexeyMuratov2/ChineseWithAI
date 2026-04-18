package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.List;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyItem;

public interface LessonVocabularyItemRepository {
    void saveAll(List<LessonVocabularyItem> items);
}
