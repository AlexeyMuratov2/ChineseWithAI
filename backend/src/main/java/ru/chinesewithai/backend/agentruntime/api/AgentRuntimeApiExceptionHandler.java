package ru.chinesewithai.backend.agentruntime.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentModelNotFoundException;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileConfigurationException;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentProfileNotFoundException;
import ru.chinesewithai.backend.agentruntime.application.exception.AgentSessionNotFoundException;

@RestControllerAdvice
public class AgentRuntimeApiExceptionHandler {

    @ExceptionHandler(AgentProfileNotFoundException.class)
    ProblemDetail handleProfileNotFound(AgentProfileNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AgentModelNotFoundException.class)
    ProblemDetail handleModelNotFound(AgentModelNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AgentSessionNotFoundException.class)
    ProblemDetail handleSessionNotFound(AgentSessionNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AgentProfileConfigurationException.class)
    ProblemDetail handleProfileConfiguration(AgentProfileConfigurationException ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
