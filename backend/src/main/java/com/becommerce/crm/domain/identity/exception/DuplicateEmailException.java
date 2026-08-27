package com.becommerce.crm.domain.identity.exception;

/**
 * E-mail já registrado no sistema (CRM ou Keycloak).
 * Mapeado para 409 Conflict no GlobalExceptionHandler.
 */
public class DuplicateEmailException extends IllegalStateException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
