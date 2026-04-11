package ru.chinesewithai.backend.lessondraft.application.port.out;

import java.util.List;

public record LessonDraftPage(
        List<LessonDraftListItem> items, long totalElements, int totalPages, boolean hasNext) {}
