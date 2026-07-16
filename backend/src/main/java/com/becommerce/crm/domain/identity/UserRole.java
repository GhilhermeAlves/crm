package com.becommerce.crm.domain.identity;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserRole {
    private UUID userId;
    private UUID roleId;
    private UUID companyId;
    private LocalDateTime createdAt;

    public UserRole() {
    }

    public static UserRole assign(UUID userId, UUID roleId, UUID companyId) {
        UserRole userRole = new UserRole();
        userRole.userId = userId;
        userRole.roleId = roleId;
        userRole.companyId = companyId;
        userRole.createdAt = LocalDateTime.now();
        return userRole;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
