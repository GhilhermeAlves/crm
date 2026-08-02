package com.becommerce.crm.domain.identity.exception;

/**
 * Rejeição de acesso ao CRM (Sprint 6). Diferente de autenticação: o JWT do
 * Keycloak é válido, mas o usuário não pode entrar na aplicação por falha em
 * um dos gates: {@code is_active}, {@code crm_enabled} ou {@code companies.status}.
 */
public class CrmAccessDeniedException extends IllegalStateException {

    public CrmAccessDeniedException(String message) {
        super(message);
    }
}
