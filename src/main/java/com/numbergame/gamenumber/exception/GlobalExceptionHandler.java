package com.numbergame.gamenumber.exception;

import com.numbergame.gamenumber.exception.custom.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler using RFC 7807 ProblemDetail
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResourceException(DuplicateResourceException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Resource Already Exists");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/duplicate"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InsufficientTurnsException.class)
    public ProblemDetail handleInsufficientTurnsException(InsufficientTurnsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Insufficient Turns");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/insufficient-turns"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("action", "buy-more-turns");
        return problemDetail;
    }

    @ExceptionHandler(GameLockException.class)
    public ProblemDetail handleGameLockException(GameLockException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Game Lock Conflict");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/game-lock"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("retryAfter", 2);
        return problemDetail;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Credentials");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/invalid-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/bad-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.gamenumber.com/errors/internal"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
