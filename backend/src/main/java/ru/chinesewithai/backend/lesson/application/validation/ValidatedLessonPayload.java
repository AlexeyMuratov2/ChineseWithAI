package ru.chinesewithai.backend.lesson.application.validation;

import java.util.List;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyWord;

public record ValidatedLessonPayload(
        String moduleKey,
        int schemaVersion,
        String title,
        String studyLanguage,
        String explanationLanguage,
        String translationLanguage,
        List<LessonVocabularyWord> newWords,
        String contentJson) {

    public ValidatedLessonPayload {
        newWords = List.copyOf(newWords);
    }
}
