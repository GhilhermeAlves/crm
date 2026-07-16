package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findById(UUID id);
    Optional<Permission> findByName(String name);
    List<Permission> findAll();
    List<Permission> findByModule(String module);
    List<Permission> findByRoleId(UUID roleId);
    void deleteById(UUID id);
    boolean existsByName(String name);
}
