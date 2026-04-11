package ru.chinesewithai.backend.lessondraft.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLessonDraftRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @Size(max = 4000) String userInstructions,
        @Size(max = 35) String explanationLanguage,
        @Size(max = 35) String translationLanguage) {}
