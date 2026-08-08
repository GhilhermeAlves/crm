package com.becommerce.crm.domain.identity.exception;

/**
 * Identidade externa (ex.: Google) cujo e-mail corresponde a uma conta local
 * existente, porém sem {@code keycloak_sub} vinculado (Sprint 7.2).
 *
 * <p>NUNCA auto-vincular por e-mail: exige verificação explícita da conta local
 * (senha) antes de gravar o {@code keycloak_sub}. O endpoint interno mapeia para
 * {@code 409 LINKING_REQUIRED}.
 */
public class LinkingRequiredException extends RuntimeException {
    public LinkingRequiredException(String message) {
        super(message);
    }
}
