package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;

public interface LearnerVocabularyProgressRepository {

    Optional<LearnerVocabularyProgress> findByUserIdAndHanziAndPinyinAndTranslationLanguage(
            UUID userId, String hanzi, String pinyin, LanguageTag translationLanguage);

    LearnerVocabularyProgress save(LearnerVocabularyProgress progress);

    List<LearnerVocabularyProgress> findByUserIdAndTranslationLanguageAndStatusIn(
            UUID userId, LanguageTag translationLanguage, Set<LearnerVocabularyStatus> statuses);
}
