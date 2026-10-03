package com.becommerce.crm.masterdata.company.domain;

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
