package com.becommerce.crm.application.membership.port.output;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeção de uma membership do usuário (todas as empresas, via RLS de linha própria).
 */
public interface MembershipProjection {

    UUID getCompanyId();

    String getCompanyName();

    String getRole();

    String getStatus();

    LocalDateTime getJoinedAt();
}
