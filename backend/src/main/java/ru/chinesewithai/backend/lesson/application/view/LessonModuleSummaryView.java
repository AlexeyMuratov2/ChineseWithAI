package ru.chinesewithai.backend.lesson.application.view;

public record LessonModuleSummaryView(
        String moduleKey, String displayName, int schemaVersion, boolean active) {}
