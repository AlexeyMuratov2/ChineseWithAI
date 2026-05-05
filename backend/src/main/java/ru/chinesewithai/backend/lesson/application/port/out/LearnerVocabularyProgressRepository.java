package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;

public interface LearnerVocabularyProgressRepository {

    Optional<LearnerVocabularyProgress> findByHanziAndPinyinAndTranslationLanguage(
            String hanzi, String pinyin, LanguageTag translationLanguage);

    LearnerVocabularyProgress save(LearnerVocabularyProgress progress);

    List<LearnerVocabularyProgress> findByTranslationLanguageAndStatusIn(
            LanguageTag translationLanguage, Set<LearnerVocabularyStatus> statuses);
}
