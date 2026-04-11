package ru.chinesewithai.backend.lessondraft.application.command;

public record ListMyLessonDraftsQuery(int page, int size) {

    public ListMyLessonDraftsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
