package com.becommerce.crm.identity.infrastructure.client.dto;

/**
 * Identidade autenticada sem usuário CRM ({@code PROVISIONING_REQUIRED}).
 */
public record ProvisioningIdentityDto(String sub, String email, String displayName) {
}
