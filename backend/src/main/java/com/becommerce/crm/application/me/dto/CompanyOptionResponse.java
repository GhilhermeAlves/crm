package com.becommerce.crm.application.me.dto;

import java.util.UUID;

/**
 * Empresa retornada no seletor de empresas do usuário (Sprint 8.4 - Company Switcher).
 *
 * <p>Somente empresas com {@code memberships} {@code ACTIVE} do usuário são
 * expostas. O flag {@code active} identifica a empresa ativa corrente, resolvida
 * pelo backend a partir de {@code users.company_id} — nunca pela resposta do
 * frontend.
 */
public record CompanyOptionResponse(
        UUID companyId,
        String name,
        String logo,
        boolean active) {
}