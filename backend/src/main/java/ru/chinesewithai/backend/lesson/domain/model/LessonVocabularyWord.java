package ru.chinesewithai.backend.lesson.domain.model;

import java.util.Objects;

public record LessonVocabularyWord(String hanzi, String pinyin, String translation) {

    public LessonVocabularyWord {
        hanzi = requireText(hanzi, "hanzi");
        pinyin = requireText(pinyin, "pinyin");
        translation = requireText(translation, "translation");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
