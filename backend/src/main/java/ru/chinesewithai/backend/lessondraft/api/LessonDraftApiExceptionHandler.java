package ru.chinesewithai.backend.lessondraft.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.OptimisticLockException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chinesewithai.backend.lessondraft.application.exception.InvalidSourcePayloadException;
import ru.chinesewithai.backend.lessondraft.application.exception.LessonDraftNotFoundException;
import ru.chinesewithai.backend.lessondraft.application.exception.SourceNotFoundException;
import ru.chinesewithai.backend.lessondraft.application.exception.SourceOrderMismatchException;

@RestControllerAdvice
public class LessonDraftApiExceptionHandler {

    @ExceptionHandler(LessonDraftNotFoundException.class)
    ProblemDetail handleLessonDraftNotFound(LessonDraftNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(SourceNotFoundException.class)
    ProblemDetail handleSourceNotFound(SourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidSourcePayloadException.class, SourceOrderMismatchException.class})
    ProblemDetail handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    ProblemDetail handleOptimisticLock(Exception ex, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Lesson draft was changed by another request. Please reload and retry.",
                request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
