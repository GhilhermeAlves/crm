package com.becommerce.crm.application.membership.port.output;

import com.becommerce.crm.domain.membership.Membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para a fonte de verdade {@code memberships}.
 *
 * <p>O isolamento por tenant e a visão de "linha própria" são garantidos pelo
 * RLS FORCE na tabela (V030); os métodos desta interface respeitam as policies.
 */
public interface MembershipRepository {

    Membership save(Membership membership);

    Optional<Membership> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    Optional<Membership> findActiveByUserIdAndCompanyId(UUID userId, UUID companyId);

    List<Membership> findByUserId(UUID userId);

    Optional<String> findMembershipRoleByUserIdAndCompanyId(UUID userId, UUID companyId);

    List<MemberProjection> findActiveMembersByCompanyId(UUID companyId);

    List<MembershipProjection> findMembershipsByUserId(UUID userId);

    long countActiveByCompanyId(UUID companyId);

    long countActiveAdminByCompanyId(UUID companyId);

    boolean existsActiveByUserIdAndCompanyId(UUID userId, UUID companyId);
}
