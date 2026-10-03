package com.becommerce.crm.identity.membership.domain;

public enum MembershipStatus {
    ACTIVE,
    PENDING,
    REMOVED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
