package ru.chinesewithai.backend.lessondraft.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceType;

public record AddLessonDraftSourceRequest(
        @NotNull LessonDraftSourceType type,
        @Size(max = 20000) String textContent,
        UUID documentFileId,
        @Size(max = 255) String documentOriginalFileName) {}
