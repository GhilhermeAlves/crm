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
}
