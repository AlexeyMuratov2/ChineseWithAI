package ru.chinesewithai.backend.lesson.application.validation;

public record ValidatedLessonPayload(
        String moduleKey,
        int schemaVersion,
        String title,
        String studyLanguage,
        String explanationLanguage,
        String translationLanguage,
        String contentJson) {}
