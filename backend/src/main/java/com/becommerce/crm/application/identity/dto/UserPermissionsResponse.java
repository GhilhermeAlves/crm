package com.becommerce.crm.application.identity.dto;

import java.util.List;
import java.util.UUID;

/**
 * Permissões de um usuário (Sprint 20 — Fase 2).
 *
 * <p>{@code effective} = permissões finais (perfis ∪ ALLOW) − DENY.
 * {@code overrides} = apenas as linhas de {@code user_permissions} do usuário,
 * para o administrador entender DE ONDE vem cada permissão.
 */
public record UserPermissionsResponse(
        UUID userId,
        List<String> roles,
        List<String> effective,
        List<OverrideEntry> overrides
) {

    public record OverrideEntry(String permissionName, String effect) {
    }
}
