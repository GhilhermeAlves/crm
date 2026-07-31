package com.becommerce.auth.presentation.rest.dto;

import com.becommerce.auth.domain.identity.AuthenticatedIdentity;

/**
 * Identidade autenticada ecoada no estado {@code PROVISIONING_REQUIRED}. É
 * derivada do JWT (contexto autenticado), nunca de entrada do cliente.
 */
public record ProvisioningIdentityResponse(
        String keycloakSub,
        String email,
        String displayName) {

    public static ProvisioningIdentityResponse from(AuthenticatedIdentity identity) {
        return new ProvisioningIdentityResponse(identity.keycloakSub(), identity.email(), identity.displayName());
    }
}
