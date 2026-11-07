package com.becommerce.crm.masterdata.company.domain;

/** Planos comerciais e seus limites padrão; a empresa pode ter limites negociados. */
public enum CompanyPlan {
    STARTER(2, 5_120, 2_000),
    PROFESSIONAL(5, 25_600, 10_000),
    BUSINESS(15, 51_200, 50_000),
    ENTERPRISE(50, 512_000, 200_000);

    private final int defaultMaxUsers;
    private final int defaultMaxStorageMb;
    private final int defaultMaxContacts;

    CompanyPlan(int defaultMaxUsers, int defaultMaxStorageMb, int defaultMaxContacts) {
        this.defaultMaxUsers = defaultMaxUsers;
        this.defaultMaxStorageMb = defaultMaxStorageMb;
        this.defaultMaxContacts = defaultMaxContacts;
    }

    public int defaultMaxUsers() { return defaultMaxUsers; }
    public int defaultMaxStorageMb() { return defaultMaxStorageMb; }
    public int defaultMaxContacts() { return defaultMaxContacts; }

    public boolean canAccessAdvancedFeatures() {
        return this == PROFESSIONAL || this == BUSINESS || this == ENTERPRISE;
    }

    public boolean canAccessEnterpriseFeatures() {
        return this == ENTERPRISE;
    }
}
