package com.becommerce.crm.infrastructure.membership.persistence;

import com.becommerce.crm.application.me.port.output.MyCompanyProjection;
import com.becommerce.crm.application.membership.port.output.MemberProjection;
import com.becommerce.crm.application.membership.port.output.MembershipProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMembershipRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    Optional<MembershipJpaEntity> findFirstByUserIdAndCompanyIdOrderByJoinedAt(UUID userId, UUID companyId);

    Optional<MembershipJpaEntity> findFirstByUserIdAndCompanyIdAndStatusOrderByJoinedAt(
            UUID userId, UUID companyId, String status);

    List<MembershipJpaEntity> findByUserId(UUID userId);

    List<MembershipJpaEntity> findByCompanyIdAndStatus(UUID companyId, String status);

    long countByCompanyIdAndStatus(UUID companyId, String status);

    @Query(value = """
            SELECT COUNT(*)
            FROM memberships m
            WHERE m.company_id = :companyId
              AND m.status = 'ACTIVE'
              AND m.role IN ('ADMIN', 'OWNER', 'SUPER_ADMIN')
            """, nativeQuery = true)
    long countActiveAdminsByCompanyId(@Param("companyId") UUID companyId);

    @Query(value = """
            SELECT m.role AS role
            FROM memberships m
            WHERE m.user_id = :userId
              AND m.company_id = :companyId
              AND m.status = 'ACTIVE'
            """, nativeQuery = true)
    Optional<String> findMembershipRoleByUserIdAndCompanyId(
            @Param("userId") UUID userId, @Param("companyId") UUID companyId);

    @Query(value = """
            SELECT m.user_id AS userId,
                   m.role    AS role,
                   m.joined_at AS joinedAt,
                   u.name    AS name,
                   u.email   AS email
            FROM memberships m
            JOIN users u ON u.id = m.user_id
            WHERE m.company_id = :companyId
              AND m.status = 'ACTIVE'
            ORDER BY u.name
            """, nativeQuery = true)
    List<MemberProjection> findActiveMembersByCompanyId(@Param("companyId") UUID companyId);

    @Query(value = """
            SELECT m.company_id AS companyId,
                   c.trading_name AS companyName,
                   m.role    AS role,
                   m.status  AS status,
                   m.joined_at AS joinedAt
            FROM memberships m
            JOIN companies c ON c.id = m.company_id
            WHERE m.user_id = :userId
            ORDER BY m.joined_at
            """, nativeQuery = true)
    List<MembershipProjection> findMembershipsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndCompanyIdAndStatus(UUID userId, UUID companyId, String status);

    @Query(value = """
            SELECT m.company_id AS companyId,
                   c.trading_name AS companyName,
                   c.logo_url     AS logoUrl,
                   m.role    AS role
            FROM memberships m
            JOIN companies c ON c.id = m.company_id
            WHERE m.user_id = :userId
              AND m.status = 'ACTIVE'
            ORDER BY m.joined_at
            """, nativeQuery = true)
    List<MyCompanyProjection> findActiveCompanyOptionsByUserId(@Param("userId") UUID userId);
}
