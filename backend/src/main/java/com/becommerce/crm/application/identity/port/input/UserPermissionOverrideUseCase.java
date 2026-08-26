package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.UserPermissionsResponse;

import java.util.UUID;

/**
 * Overrides individuais de permissão por usuário (Sprint 20 — Fase 2).
 * Política: efetiva = (perfis ∪ ALLOW) − DENY; INHERIT = ausência de linha.
 */
public interface UserPermissionOverrideUseCase {

    UserPermissionsResponse listPermissions(UUID companyId, UUID userId);

    /** Cria/atualiza o override (effect: ALLOW | DENY). */
    void setOverride(UUID companyId, UUID userId, UUID permissionId, String effect);

    /** Remove o override → volta a INHERIT. */
    void removeOverride(UUID companyId, UUID userId, UUID permissionId);
}
