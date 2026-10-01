package com.becommerce.crm.identity.domain;

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
