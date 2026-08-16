package com.thurdass.system2a.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: method={} path={}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(
            BusinessException exception,
            HttpServletRequest request
    ) {
        log.warn("Business rule rejected request: method={} path={}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> validation(Exception exception, HttpServletRequest request) {
        log.warn("Validation rejected request: method={} path={}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, "Invalid request data", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("Access denied: method={} path={}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException exception, HttpServletRequest request) {
        log.warn("Authentication rejected: method={} path={}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected server error: method={} path={}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus httpStatus, String message, HttpServletRequest request) {
        return ResponseEntity.status(httpStatus).body(
                new ApiError(
                        LocalDateTime.now(),
                        httpStatus.value(),
                        httpStatus.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                )
        );
    }
}
