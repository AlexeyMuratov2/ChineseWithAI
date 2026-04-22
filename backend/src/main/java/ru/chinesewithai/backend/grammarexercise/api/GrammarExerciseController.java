package ru.chinesewithai.backend.grammarexercise.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.config.OpenApiConfig;
import ru.chinesewithai.backend.grammarexercise.api.dto.GenerateGrammarExerciseRequest;
import ru.chinesewithai.backend.grammarexercise.api.dto.GrammarExerciseGenerationResponse;
import ru.chinesewithai.backend.grammarexercise.api.dto.GrammarExerciseItemRequest;
import ru.chinesewithai.backend.grammarexercise.application.command.GenerateGrammarExerciseCommand;
import ru.chinesewithai.backend.grammarexercise.application.command.GrammarExerciseItemCommand;
import ru.chinesewithai.backend.grammarexercise.application.port.in.GenerateGrammarExerciseUseCase;
import ru.chinesewithai.backend.grammarexercise.application.view.GrammarExerciseView;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/grammar-exercises")
public class GrammarExerciseController {

    private final GenerateGrammarExerciseUseCase generateGrammarExerciseUseCase;

    public GrammarExerciseController(GenerateGrammarExerciseUseCase generateGrammarExerciseUseCase) {
        this.generateGrammarExerciseUseCase = generateGrammarExerciseUseCase;
    }

    @PostMapping("/generate")
    public ResponseEntity<GrammarExerciseGenerationResponse> generate(
            @Valid @RequestBody GenerateGrammarExerciseRequest request) {
        var view = generateGrammarExerciseUseCase.generate(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(view));
    }

    private GenerateGrammarExerciseCommand toCommand(GenerateGrammarExerciseRequest request) {
        return new GenerateGrammarExerciseCommand(
                request.explanationLanguage(),
                request.modelKey(),
                request.items().stream().map(this::toCommand).toList());
    }

    private GrammarExerciseItemCommand toCommand(GrammarExerciseItemRequest request) {
        return new GrammarExerciseItemCommand(request.term(), request.focus());
    }

    private GrammarExerciseGenerationResponse toResponse(GrammarExerciseView view) {
        return new GrammarExerciseGenerationResponse(view.generatorSessionId(), view.content());
    }
}
