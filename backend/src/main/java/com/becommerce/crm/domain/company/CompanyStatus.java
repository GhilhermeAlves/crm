package com.becommerce.crm.domain.company;

public enum CompanyStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    ONBOARDING;

    public boolean canOperate() {
        return this == ACTIVE;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
