package com.becommerce.auth.domain.identity.exception;

/**
 * Rejeição de usuário desativado/inativo durante a resolução do CurrentUser.
 * Regra paritária ao provisionamento do crm-backend (Sprint 1): usuário
 * {@code INACTIVE} é negado (401) mesmo com JWT válido.
 */
public class UserInactiveException extends RuntimeException {

    public UserInactiveException(String message) {
        super(message);
    }
}
