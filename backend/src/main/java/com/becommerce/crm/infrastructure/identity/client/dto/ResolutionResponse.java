package com.becommerce.crm.infrastructure.identity.client.dto;

/**
 * Contrato discriminado de {@code GET /internal/auth/current-user} do
 * crm-auth-service: {@code RESOLVED} (com {@code currentUser}) ou
 * {@code PROVISIONING_REQUIRED} (com {@code identity}).
 */
public record ResolutionResponse(
        String status,
        CurrentUserDto currentUser,
        ProvisioningIdentityDto identity) {

    public boolean isResolved() {
        return "RESOLVED".equals(status) && currentUser != null;
    }
}
