package ru.chinesewithai.backend.lesson.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.chinesewithai.backend.lesson.application.port.out.LearnerVocabularyProgressRepository;
import ru.chinesewithai.backend.lesson.application.port.out.LessonVocabularyItemRepository;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyItem;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyWord;

@Service
public class LessonVocabularyTrackingService {

    private final LessonVocabularyItemRepository lessonVocabularyItemRepository;
    private final LearnerVocabularyProgressRepository learnerVocabularyProgressRepository;

    public LessonVocabularyTrackingService(
            LessonVocabularyItemRepository lessonVocabularyItemRepository,
            LearnerVocabularyProgressRepository learnerVocabularyProgressRepository) {
        this.lessonVocabularyItemRepository = lessonVocabularyItemRepository;
        this.learnerVocabularyProgressRepository = learnerVocabularyProgressRepository;
    }

    public void recordLessonVocabulary(Lesson lesson, List<LessonVocabularyWord> words) {
        if (words == null || words.isEmpty()) {
            return;
        }

        var uniqueWords = new LinkedHashMap<String, LessonVocabularyWord>();
        for (var word : words) {
            uniqueWords.putIfAbsent(key(word.hanzi(), word.pinyin(), lesson.translationLanguage().value()), word);
        }

        lessonVocabularyItemRepository.saveAll(uniqueWords.values().stream()
                .map(word -> LessonVocabularyItem.createNew(
                        lesson.id(),
                        lesson.ownerId(),
                        word.hanzi(),
                        word.pinyin(),
                        word.translation(),
                        lesson.translationLanguage(),
                        lesson.createdAt()))
                .toList());

        for (var word : uniqueWords.values()) {
            var existing = learnerVocabularyProgressRepository.findByUserIdAndHanziAndPinyinAndTranslationLanguage(
                    lesson.ownerId(), word.hanzi(), word.pinyin(), lesson.translationLanguage());
            var progress = existing
                    .map(value -> value.refreshTranslation(word.translation(), lesson.createdAt()))
                    .orElseGet(() -> LearnerVocabularyProgress.createNew(
                            lesson.ownerId(),
                            word.hanzi(),
                            word.pinyin(),
                            word.translation(),
                            lesson.translationLanguage(),
                            lesson.createdAt()));
            learnerVocabularyProgressRepository.save(progress);
        }
    }

    private String key(String hanzi, String pinyin, String translationLanguage) {
        return hanzi + "|" + pinyin + "|" + translationLanguage;
    }
}
