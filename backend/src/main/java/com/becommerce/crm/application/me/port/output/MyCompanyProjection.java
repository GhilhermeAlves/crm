package com.becommerce.crm.application.me.port.output;

import java.util.UUID;

/**
 * Projeção de uma empresa na qual o usuário possui {@code memberships} ativa,
 * usada pelo seletor de empresas (Sprint 8.4). Respeita o RLS: o usuário só
 * vê as próprias memberships.
 */
public interface MyCompanyProjection {

    UUID getCompanyId();

    String getCompanyName();

    String getLogoUrl();

    String getRole();
}