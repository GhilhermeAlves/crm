package com.becommerce.crm.application.membership.port.output;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeção de membro ativo de uma empresa (membership + user).
 */
public interface MemberProjection {

    UUID getUserId();

    String getRole();

    LocalDateTime getJoinedAt();

    String getName();

    String getEmail();
}
