package com.becommerce.crm.domain.identity;

import java.time.LocalDateTime;
import java.util.UUID;

public class RolePermission {
    private UUID id;
    private UUID roleId;
    private UUID permissionId;
    private LocalDateTime createdAt;

    public RolePermission() {}

    public static RolePermission create(UUID roleId, UUID permissionId) {
        RolePermission rp = new RolePermission();
        rp.id = UUID.randomUUID();
        rp.roleId = roleId;
        rp.permissionId = permissionId;
        rp.createdAt = LocalDateTime.now();
        return rp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public UUID getPermissionId() { return permissionId; }
    public void setPermissionId(UUID permissionId) { this.permissionId = permissionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
