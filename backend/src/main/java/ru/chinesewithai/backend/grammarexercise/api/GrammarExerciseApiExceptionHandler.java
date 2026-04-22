package ru.chinesewithai.backend.grammarexercise.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chinesewithai.backend.grammarexercise.application.exception.GrammarExerciseGenerationFailedException;

@RestControllerAdvice(assignableTypes = GrammarExerciseController.class)
public class GrammarExerciseApiExceptionHandler {

    @ExceptionHandler(GrammarExerciseGenerationFailedException.class)
    ProblemDetail handleGenerationFailed(GrammarExerciseGenerationFailedException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
