package ru.chinesewithai.backend.lesson.application.source;

import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public record LessonSourceProcessingRequest(LessonDraftView draft, LessonSourceProcessingPolicy policy) {}
