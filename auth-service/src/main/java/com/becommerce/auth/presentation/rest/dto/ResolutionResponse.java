package com.becommerce.auth.presentation.rest.dto;

import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;

/**
 * Contrato de resposta da resolução do CurrentUser. Discriminado por
 * {@code status}:
 *
 * <ul>
 *   <li>{@code RESOLVED} — {@code currentUser} completo;</li>
 *   <li>{@code PROVISIONING_REQUIRED} — identidade autenticada sem usuário CRM
 *       (contrato preparado para o provisionamento que migrará para o
 *       auth-service).</li>
 * </ul>
 */
public record ResolutionResponse(
        String status,
        CurrentUserResponse currentUser,
        ProvisioningIdentityResponse identity) {

    public static ResolutionResponse from(CurrentUserResolution resolution) {
        if (resolution instanceof CurrentUserResolution.Resolved resolved) {
            return resolved(resolved.currentUser());
        }
        CurrentUserResolution.ProvisioningRequired required =
                (CurrentUserResolution.ProvisioningRequired) resolution;
        return provisioningRequired(required.identity());
    }

    public static ResolutionResponse resolved(CurrentUser currentUser) {
        return new ResolutionResponse("RESOLVED", CurrentUserResponse.from(currentUser), null);
    }

    public static ResolutionResponse provisioningRequired(AuthenticatedIdentity identity) {
        return new ResolutionResponse("PROVISIONING_REQUIRED", null, ProvisioningIdentityResponse.from(identity));
    }
}
