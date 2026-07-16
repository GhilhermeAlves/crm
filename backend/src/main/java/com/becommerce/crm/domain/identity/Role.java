package com.becommerce.crm.domain.identity;

import com.becommerce.crm.domain.identity.valueobject.RoleName;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Role {
    private UUID id;
    private RoleName name;
    private UUID companyId;
    private Set<String> permissions;
    private LocalDateTime createdAt;

    public Role() {
    }

    public static Role create(RoleName name, UUID companyId) {
        Role role = new Role();
        role.id = UUID.randomUUID();
        role.name = name;
        role.companyId = companyId;
        role.permissions = new HashSet<>();
        role.createdAt = LocalDateTime.now();
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

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
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
}
