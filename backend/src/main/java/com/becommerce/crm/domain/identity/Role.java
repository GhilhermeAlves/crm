package com.becommerce.crm.domain.identity;

import com.becommerce.crm.domain.identity.valueobject.RoleName;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Role {
    private UUID id;
    private RoleName name;
    private String description;
    private UUID companyId;
    private boolean isSystem;
    private boolean isActive;
    private Set<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Role() {
    }

    public static Role create(RoleName name, UUID companyId) {
        Role role = new Role();
        role.id = UUID.randomUUID();
        role.name = name;
        role.companyId = companyId;
        role.description = name.getDisplayName();
        role.isSystem = false;
        role.isActive = true;
        role.permissions = new HashSet<>();
        role.createdAt = LocalDateTime.now();
        role.updatedAt = LocalDateTime.now();
        return role;
    }

    public static final UUID SYSTEM_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static Role createSystem(RoleName name) {
        Role role = new Role();
        role.id = UUID.randomUUID();
        role.name = name;
        role.companyId = SYSTEM_COMPANY_ID;
        role.description = name.getDisplayName() + " (System Role)";
        role.isSystem = true;
        role.isActive = true;
        role.permissions = new HashSet<>();
        role.createdAt = LocalDateTime.now();
        role.updatedAt = LocalDateTime.now();
        return role;
    }

    public void addPermission(String permission) {
        this.permissions.add(permission);
    }

    public void removePermission(String permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        return this.permissions.contains(permission);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoleName getName() {
        return name;
    }

    public void setName(RoleName name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
