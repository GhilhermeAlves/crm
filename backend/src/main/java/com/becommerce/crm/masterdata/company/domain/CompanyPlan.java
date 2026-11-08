package com.becommerce.crm.masterdata.company.domain;

/** Planos comerciais e seus limites padrão; a empresa pode ter limites negociados. */
public enum CompanyPlan {
    ESSENTIAL(1, 1_024, 500, 4_990),
    STARTER(2, 5_120, 2_000, 19_700),
    PROFESSIONAL(5, 25_600, 10_000, 39_700),
    BUSINESS(15, 51_200, 50_000, 79_700),
    ENTERPRISE(50, 512_000, 200_000, 150_000);

    private final int defaultMaxUsers;
    private final int defaultMaxStorageMb;
    private final int defaultMaxContacts;
    private final int defaultMonthlyPriceCents;

    CompanyPlan(int defaultMaxUsers, int defaultMaxStorageMb, int defaultMaxContacts,
             int defaultMonthlyPriceCents) {
        this.defaultMaxUsers = defaultMaxUsers;
        this.defaultMaxStorageMb = defaultMaxStorageMb;
        this.defaultMaxContacts = defaultMaxContacts;
        this.defaultMonthlyPriceCents = defaultMonthlyPriceCents;
    }

    public int defaultMaxUsers() { return defaultMaxUsers; }
    public int defaultMaxStorageMb() { return defaultMaxStorageMb; }
    public int defaultMaxContacts() { return defaultMaxContacts; }
    public int defaultMonthlyPriceCents() { return defaultMonthlyPriceCents; }

    public boolean canAccessAdvancedFeatures() {
        return this == PROFESSIONAL || this == BUSINESS || this == ENTERPRISE;
    }

    public boolean canAccessEnterpriseFeatures() {
        return this == ENTERPRISE;
    }
}
