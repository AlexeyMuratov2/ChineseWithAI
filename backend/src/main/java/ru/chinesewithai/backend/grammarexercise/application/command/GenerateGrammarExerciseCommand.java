package ru.chinesewithai.backend.grammarexercise.application.command;

import java.util.List;

public record GenerateGrammarExerciseCommand(
        String explanationLanguage, String modelKey, List<GrammarExerciseItemCommand> items) {

    public GenerateGrammarExerciseCommand {
        items = List.copyOf(items);
    }
}
