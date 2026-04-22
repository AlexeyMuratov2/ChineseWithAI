package ru.chinesewithai.backend.grammarexercise.application.view;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record GrammarExerciseView(UUID generatorSessionId, JsonNode content) {}
