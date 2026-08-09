package com.becommerce.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Leitura de memberships (schema compartilhado, RLS FORCE por tenant/linha
 * própria — V030). Usada pela resolução do CurrentUser.
 */
@Repository
public interface SpringDataMembershipRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    @Query(value = """
            SELECT m.role
            FROM memberships m
            WHERE m.user_id = :userId
              AND m.company_id = :companyId
              AND m.status = 'ACTIVE'
            """, nativeQuery = true)
    Optional<String> findMembershipRoleByUserIdAndCompanyId(
            @Param("userId") UUID userId, @Param("companyId") UUID companyId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM memberships m
                WHERE m.user_id = :userId
                  AND m.company_id = :companyId
                  AND m.status = 'ACTIVE'
            )
            """, nativeQuery = true)
    boolean existsActiveByUserIdAndCompanyId(
            @Param("userId") UUID userId, @Param("companyId") UUID companyId);
}
