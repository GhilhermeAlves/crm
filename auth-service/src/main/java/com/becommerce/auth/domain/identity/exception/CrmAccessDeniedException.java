package com.becommerce.auth.domain.identity.exception;

/**
 * Rejeição de acesso ao CRM (Sprint 6): o JWT do Keycloak é válido, mas o
 * usuário não pode entrar na aplicação por falha em um dos gates
 * ({@code is_active}, {@code crm_enabled} ou {@code companies.status}).
 * Mapeado para {@code 403 CRM_ACCESS_DENIED}.
 */
public class CrmAccessDeniedException extends RuntimeException {

    public CrmAccessDeniedException(String message) {
        super(message);
    }
}
