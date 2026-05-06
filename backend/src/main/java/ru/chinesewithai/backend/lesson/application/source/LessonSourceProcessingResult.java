package ru.chinesewithai.backend.lesson.application.source;

public record LessonSourceProcessingResult(
        LessonSourceProcessingMode mode,
        LessonSourceBundle sourceBundle,
        LessonSourcePack sourcePack) {}
