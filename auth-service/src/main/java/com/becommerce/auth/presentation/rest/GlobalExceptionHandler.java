package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.auth.domain.identity.exception.KeycloakUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ExceptionHandler(KeycloakUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakUnavailable(KeycloakUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("KEYCLOAK_UNAVAILABLE", e.getMessage()));
    }

    public record ErrorResponse(String code, String message) {}
}
