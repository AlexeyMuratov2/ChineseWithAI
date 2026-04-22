package ru.chinesewithai.backend.grammarexercise.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GenerateGrammarExerciseRequest(
        @Size(max = 35) String explanationLanguage,
        @Size(max = 120) String modelKey,
        @NotEmpty List<@Valid GrammarExerciseItemRequest> items) {}
