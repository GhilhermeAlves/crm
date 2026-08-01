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
}
