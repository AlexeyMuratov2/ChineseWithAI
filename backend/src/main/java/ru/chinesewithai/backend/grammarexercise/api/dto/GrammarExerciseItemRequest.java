package ru.chinesewithai.backend.grammarexercise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrammarExerciseItemRequest(@NotBlank @Size(max = 255) String term, @NotBlank @Size(max = 1000) String focus) {}
