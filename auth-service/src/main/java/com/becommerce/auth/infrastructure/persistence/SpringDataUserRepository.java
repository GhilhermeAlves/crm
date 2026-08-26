package com.becommerce.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data de leitura sobre o schema CRM compartilhado. Consultas
 * de RBAC são joins diretos nas tabelas de identidade (roles, user_roles,
 * role_permissions, permissions).
 */
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByKeycloakSub(String keycloakSub);

    Optional<UserJpaEntity> findByEmail(String email);

    @Query(value = """
            SELECT r.name
            FROM roles r
            INNER JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId AND ur.company_id = :companyId
            ORDER BY r.name
            """, nativeQuery = true)
    List<String> findRoleNamesByUserIdAndCompanyId(@Param("userId") UUID userId, @Param("companyId") UUID companyId);

    @Query(value = """
            SELECT name FROM (
                SELECT DISTINCT p.id, p.name
                FROM permissions p
                INNER JOIN role_permissions rp ON rp.permission_id = p.id
                INNER JOIN user_roles ur ON ur.role_id = rp.role_id
                WHERE ur.user_id = :userId AND ur.company_id = :companyId
                UNION
                SELECT p.id, p.name
                FROM user_permissions up
                INNER JOIN permissions p ON p.id = up.permission_id
                WHERE up.user_id = :userId AND up.company_id = :companyId
                  AND up.effect = 'ALLOW'
            ) eff
            WHERE id NOT IN (
                SELECT up.permission_id FROM user_permissions up
                WHERE up.user_id = :userId AND up.company_id = :companyId
                  AND up.effect = 'DENY'
            )
            ORDER BY name
            """, nativeQuery = true)
    List<String> findPermissionNamesByUserIdAndCompanyId(@Param("userId") UUID userId, @Param("companyId") UUID companyId);
}
