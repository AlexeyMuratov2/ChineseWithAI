package ru.chinesewithai.backend.grammarexercise.application.port.in;

import ru.chinesewithai.backend.grammarexercise.application.command.GenerateGrammarExerciseCommand;
import ru.chinesewithai.backend.grammarexercise.application.view.GrammarExerciseView;

public interface GenerateGrammarExerciseUseCase {
    GrammarExerciseView generate(GenerateGrammarExerciseCommand command);
}
