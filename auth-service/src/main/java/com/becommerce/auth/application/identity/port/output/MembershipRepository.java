package com.becommerce.auth.application.identity.port.output;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para leitura de memberships (Sprint 8.2). Apenas leitura: a
 * gestão de membros continua no crm-backend.
 */
public interface MembershipRepository {

    Optional<String> findMembershipRoleByUserIdAndCompanyId(UUID userId, UUID companyId);

    boolean existsActiveByUserIdAndCompanyId(UUID userId, UUID companyId);
}
