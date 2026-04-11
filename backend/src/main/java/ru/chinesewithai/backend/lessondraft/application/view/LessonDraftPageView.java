package ru.chinesewithai.backend.lessondraft.application.view;

import java.util.List;

public record LessonDraftPageView(
        List<LessonDraftSummaryView> items, int page, int size, long totalElements, int totalPages, boolean hasNext) {}
