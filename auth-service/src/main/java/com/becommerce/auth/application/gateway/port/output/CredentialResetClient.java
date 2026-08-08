package com.becommerce.auth.application.gateway.port.output;

/**
 * Porta de saída para o reset de credencial via Keycloak Admin REST (Sprint 7.4).
 * O auth-service é dono da integração com o Keycloak: obtém um access token de
 * service account (client_credentials de um client confidencial de admin) e
 * chama o Admin REST do realm para redefinir a senha de um usuário de verdade.
 *
 * <p>Apenas código do servidor (crm-backend via {@code /internal/auth/reset-password})
 * utiliza esta entrada — o browser nunca tem acesso.
 */
public interface CredentialResetClient {

    /**
     * Redefine a senha do usuário do Keycloak. A identidade é resolvida, em
     * ordem de robustez: pelo {@code keycloakSub} (id do usuário no realm) ou,
     * na ausência dele, pela busca exata por e-mail no Admin REST.
     *
     * @param keycloakSub subject ({@code sub}) do usuário no Keycloak, ou vazio/null
     * @param email       e-mail do usuário (obrigatório para busca exata quando sem sub)
     * @param newPassword nova senha em texto puro — NUNCA logada, só enviada no corpo da requisição
     */
    void resetPassword(String keycloakSub, String email, String newPassword);
}