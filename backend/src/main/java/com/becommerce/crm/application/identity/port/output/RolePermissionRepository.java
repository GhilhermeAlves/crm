package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.RolePermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository {
    RolePermission save(RolePermission rolePermission);
    Optional<RolePermission> findById(UUID id);
    List<RolePermission> findByRoleId(UUID roleId);
    List<RolePermission> findByPermissionId(UUID permissionId);
    void deleteById(UUID id);
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
