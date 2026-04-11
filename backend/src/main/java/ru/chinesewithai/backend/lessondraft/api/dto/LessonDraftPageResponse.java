package ru.chinesewithai.backend.lessondraft.api.dto;

import java.util.List;

public record LessonDraftPageResponse(
        List<LessonDraftSummaryResponse> items, int page, int size, long totalElements, int totalPages, boolean hasNext) {}
