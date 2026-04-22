package ru.chinesewithai.backend.grammarexercise.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record GrammarExerciseGenerationResponse(UUID generatorSessionId, JsonNode content) {}
