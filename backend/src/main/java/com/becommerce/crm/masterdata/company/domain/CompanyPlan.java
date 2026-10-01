package com.becommerce.crm.masterdata.company.domain;

public enum CompanyPlan {
    STARTER,
    PROFESSIONAL,
    BUSINESS,
    ENTERPRISE;

    public boolean canAccessAdvancedFeatures() {
        return this == PROFESSIONAL || this == BUSINESS || this == ENTERPRISE;
    }

    public boolean canAccessEnterpriseFeatures() {
        return this == ENTERPRISE;
    }
}
