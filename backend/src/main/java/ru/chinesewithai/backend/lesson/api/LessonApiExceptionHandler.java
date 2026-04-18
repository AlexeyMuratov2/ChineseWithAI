package ru.chinesewithai.backend.lesson.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.application.exception.LessonGenerationFailedException;
import ru.chinesewithai.backend.lesson.application.exception.LessonModuleInactiveException;
import ru.chinesewithai.backend.lesson.application.exception.LessonModuleNotFoundException;
import ru.chinesewithai.backend.lesson.application.exception.LessonNotFoundException;

@RestControllerAdvice
public class LessonApiExceptionHandler {

    @ExceptionHandler({LessonNotFoundException.class, LessonModuleNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({
        LessonContentValidationException.class,
        LessonModuleInactiveException.class,
        IllegalArgumentException.class
    })
    ProblemDetail handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(LessonGenerationFailedException.class)
    ProblemDetail handleGenerationFailed(LessonGenerationFailedException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
