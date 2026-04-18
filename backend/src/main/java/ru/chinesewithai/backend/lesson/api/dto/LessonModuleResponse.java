package ru.chinesewithai.backend.lesson.api.dto;

public record LessonModuleResponse(String moduleKey, String displayName, int schemaVersion, boolean active) {}
