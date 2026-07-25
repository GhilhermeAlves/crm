package com.becommerce.crm.infrastructure.identity.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
public class UserRoleJpaEntity {

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false, insertable = false, updatable = false)
    private UUID roleId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @EmbeddedId
    private UserRoleId id;

    public UserRoleJpaEntity() {
        this.id = new UserRoleId();
    }

    @Embeddable
    public static class UserRoleId implements java.io.Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "role_id")
        private UUID roleId;

        public UserRoleId() {}

        public UserRoleId(UUID userId, UUID roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public UUID getRoleId() { return roleId; }
        public void setRoleId(UUID roleId) { this.roleId = roleId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserRoleId that)) return false;
            return java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(roleId, that.roleId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, roleId);
        }
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) {
        this.userId = userId;
        if (this.id == null) this.id = new UserRoleId();
        this.id.setUserId(userId);
    }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
        if (this.id == null) this.id = new UserRoleId();
        this.id.setRoleId(roleId);
    }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
