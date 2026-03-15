package ru.rt.rostelecom_tms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.rt.rostelecom_tms.domain.cases.exceptions.*;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanNotFoundException;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({CaseStepNotFoundException.class, CaseNotFoundException.class, CaseGroupNotFoundException.class, UserNotFoundException.class, PlanNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.getMessage(), System.currentTimeMillis()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler({
            CaseAlreadyExistsException.class,
            CaseGroupAlreadyExistsException.class,
            CaseGroupNotCreatedException.class,
            CaseGroupNotDeletableException.class,
            PlanAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleCaseConflict(RuntimeException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.getMessage(), System.currentTimeMillis()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.getMessage(), System.currentTimeMillis()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception e) {
        return new ResponseEntity<>(
                new ErrorResponse("access denied", System.currentTimeMillis()),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException e) {
        return new ResponseEntity<>(
                new ErrorResponse("unauthorized", System.currentTimeMillis()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("Unhandled exception", e);
        return new ResponseEntity<>(
                new ErrorResponse("resource already exists", System.currentTimeMillis()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " - " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return new ResponseEntity<>(
                new ErrorResponse(message, System.currentTimeMillis()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception e) {
        log.error("Internal server error", e);
        return new ResponseEntity<>(
                new ErrorResponse("internal server error", System.currentTimeMillis()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
