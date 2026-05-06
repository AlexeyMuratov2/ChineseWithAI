package ru.chinesewithai.backend.lesson.application.source;

import java.util.UUID;

public record LessonSourceRef(UUID sourceId, int position, String label) {}
