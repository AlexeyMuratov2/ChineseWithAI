package ru.chinesewithai.backend.user.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chinesewithai.backend.user.application.exception.AccountDisabledException;
import ru.chinesewithai.backend.user.application.exception.AuthenticationRequiredException;
import ru.chinesewithai.backend.user.application.exception.DuplicateUsernameException;
import ru.chinesewithai.backend.user.application.exception.InvalidCredentialsException;
import ru.chinesewithai.backend.user.application.exception.UserNotFoundException;

@RestControllerAdvice
public class UserApiExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    ProblemDetail handleDuplicateUsername(DuplicateUsernameException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    ProblemDetail handleAuthenticationRequired(AuthenticationRequiredException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    ProblemDetail handleAccountDisabled(AccountDisabledException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, details, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    private String formatFieldError(FieldError fieldError) {
        if (fieldError.getDefaultMessage() == null) {
            return fieldError.getField() + " is invalid";
        }
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
