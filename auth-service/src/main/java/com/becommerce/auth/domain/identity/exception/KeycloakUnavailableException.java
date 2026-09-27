package com.becommerce.auth.domain.identity.exception;

public class KeycloakUnavailableException extends RuntimeException {
    public KeycloakUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
