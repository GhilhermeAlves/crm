package com.becommerce.crm.identity.membership.application.port.output;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeção de membro de uma empresa (membership + user). {@code status} é o da
 * própria membership — ACTIVE na listagem padrão, REMOVED na lista de inativos.
 */
public interface MemberProjection {

    UUID getUserId();

    String getRole();

    String getStatus();

    LocalDateTime getJoinedAt();

    String getName();

    String getEmail();
}
