package com.becommerce.crm.application.identity.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Overrides individuais de permissão (Sprint 20 — Fase 2).
 * Tabela {@code user_permissions} (V065): effect ALLOW | DENY.
 * INHERIT = ausência de linha. RLS FORCE isola por company_id.
 */
public interface UserPermissionOverrideRepository {

    List<Entry> findByUser(UUID userId, UUID companyId);

    void upsert(UUID userId, UUID companyId, UUID permissionId, String effect);

    void deleteByUserIdAndPermissionId(UUID userId, UUID permissionId);

    boolean existsByUserAndPermissionAndEffect(UUID userId, UUID permissionId, String effect);

    record Entry(UUID permissionId, String permissionName, String effect) {
    }
}
