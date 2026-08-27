package com.becommerce.crm.infrastructure.identity.client;

import com.becommerce.crm.infrastructure.identity.client.dto.ResolutionResponse;

/**
 * Cliente do crm-auth-service (camada de identidade).
 */
public interface AuthServiceClient {

    /**
     * Resolve o {@code CurrentUser} a partir do JWT do Keycloak já validado.
     *
     * @param jwtToken token de acesso do Keycloak (claim {@code sub}, {@code email}, ...)
     * @return contrato discriminado ({@code RESOLVED} / {@code PROVISIONING_REQUIRED})
     */
    ResolutionResponse currentUser(String jwtToken);

    /**
     * Redefine a senha REAL do usuário no Keycloak (Sprint 7.4), via endpoint
     * interno do crm-auth-service ({@code POST /internal/auth/reset-password}),
     * autenticado por segredo de serviço (header {@code X-Internal-Api-Token}).
     *
     * @param keycloakSub subject do usuário no Keycloak, ou null/vazio para busca por e-mail
     * @param email       e-mail do usuário (obrigatório quando sem {@code keycloakSub})
     * @param newPassword nova senha em texto puro — só via corpo HTTP, jamais logada
     */
    void resetPassword(String keycloakSub, String email, String newPassword);

    /**
     * Cria um usuário no Keycloak via endpoint interno do crm-auth-service
     * ({@code POST /internal/auth/create-user}), autenticado por segredo de serviço.
     *
     * @param email    e-mail do usuário
     * @param password senha em texto puro — só via corpo HTTP, jamais logada
     * @param name     nome completo (será dividido em firstName/lastName)
     * @return ID do usuário criado no Keycloak
     */
    String createKeycloakUser(String email, String password, String name);

    /**
     * Exclui um usuário do Keycloak via endpoint interno do crm-auth-service
     * ({@code DELETE /internal/auth/create-user/{id}}), autenticado por segredo de serviço.
     * Utilizado como compensação quando a persistência no CRM falha.
     *
     * @param keycloakUserId ID do usuário no Keycloak
     */
    void deleteKeycloakUser(String keycloakUserId);
}