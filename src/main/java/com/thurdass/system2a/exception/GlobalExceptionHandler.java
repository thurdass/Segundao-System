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
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException e, HttpServletRequest r) {
        log.warn("Resource not found: method={} path={}", r.getMethod(), r.getRequestURI());
        return error(HttpStatus.NOT_FOUND, e.getMessage(), r);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(BusinessException e, HttpServletRequest r) {
        log.warn("Business rule rejected request: method={} path={}", r.getMethod(), r.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), r);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> validation(Exception e, HttpServletRequest r) {
        log.warn("Validation rejected request: method={} path={}", r.getMethod(), r.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, "Invalid request data", r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest r) {
        log.warn("Access denied: method={} path={}", r.getMethod(), r.getRequestURI());
        return error(HttpStatus.FORBIDDEN, "Access denied", r);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException e, HttpServletRequest r) {
        log.warn("Authentication rejected: method={} path={}", r.getMethod(), r.getRequestURI());
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials", r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest r) {
        log.error("Unexpected server error: method={} path={}", r.getMethod(), r.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", r);
    }

    private ResponseEntity<ApiError> error(HttpStatus s, String message, HttpServletRequest r) {
        return ResponseEntity.status(s).body(new ApiError(LocalDateTime.now(), s.value(), s.getReasonPhrase(), message, r.getRequestURI()));
    }
}
