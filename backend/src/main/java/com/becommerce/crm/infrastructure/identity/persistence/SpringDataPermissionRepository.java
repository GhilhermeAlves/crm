package com.becommerce.crm.infrastructure.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPermissionRepository extends JpaRepository<PermissionJpaEntity, UUID> {
    Optional<PermissionJpaEntity> findByName(String name);
    List<PermissionJpaEntity> findByModule(String module);
    boolean existsByName(String name);

    @Query("SELECT p FROM PermissionJpaEntity p JOIN RolePermissionJpaEntity rp ON rp.permissionId = p.id WHERE rp.roleId = :roleId")
    List<PermissionJpaEntity> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * Sprint 20 (Fase 2): resolução centralizada de permissões efetivas —
     * (perfis ∪ ALLOW) − DENY. Mesma regra do auth-service
     * (SpringDataUserRepository.findPermissionNamesByUserIdAndCompanyId).
     */
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
    List<String> findEffectivePermissionNamesByUserIdAndCompanyId(
            @Param("userId") UUID userId, @Param("companyId") UUID companyId);
}
