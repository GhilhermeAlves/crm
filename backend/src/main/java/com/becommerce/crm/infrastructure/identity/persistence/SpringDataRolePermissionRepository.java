package com.becommerce.crm.infrastructure.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRolePermissionRepository extends JpaRepository<RolePermissionJpaEntity, UUID> {
    List<RolePermissionJpaEntity> findByRoleId(UUID roleId);
    List<RolePermissionJpaEntity> findByPermissionId(UUID permissionId);
    Optional<RolePermissionJpaEntity> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
