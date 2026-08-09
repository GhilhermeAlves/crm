package com.becommerce.crm.domain.membership;

public enum MembershipStatus {
    ACTIVE,
    PENDING,
    REMOVED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
