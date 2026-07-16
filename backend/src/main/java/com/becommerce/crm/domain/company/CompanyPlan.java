package com.becommerce.crm.domain.company;

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
