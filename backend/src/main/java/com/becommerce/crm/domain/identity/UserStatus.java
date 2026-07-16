package com.becommerce.crm.domain.identity;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    PENDING;

    public boolean canOperate() {
        return this == ACTIVE;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
