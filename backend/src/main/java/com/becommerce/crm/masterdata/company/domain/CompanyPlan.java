package com.becommerce.crm.masterdata.company.domain;

import java.util.EnumSet;
import java.util.Set;

/** Planos comerciais e seus limites padrão; a empresa pode ter limites negociados. */
public enum CompanyPlan {
    ESSENTIAL(1, 1_024, 500, 12_900),
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

    /**
     * Módulos que o plano libera; cada plano inclui os do anterior. Aplicados
     * quando o plano da empresa muda — depois o SUPER_ADMIN ainda pode ligar ou
     * desligar módulos à mão.
     */
    public Set<CompanyModule> defaultModules() {
        EnumSet<CompanyModule> modules = EnumSet.of(
                CompanyModule.PATIENTS, CompanyModule.AGENDA, CompanyModule.AGENDA_SETTINGS,
                CompanyModule.CATALOG, CompanyModule.DOCUMENTS,
                CompanyModule.TASKS, CompanyModule.STORAGE);
        if (this == ESSENTIAL) return modules;
        modules.addAll(EnumSet.of(
                CompanyModule.FINANCE, CompanyModule.CRM, CompanyModule.REPORTS,
                CompanyModule.EMAIL_SETTINGS, CompanyModule.INVENTORY, CompanyModule.DATA_MIGRATION,
                CompanyModule.CREDIT_SIMULATOR, CompanyModule.CARD_FEES, CompanyModule.PATIENT_BILLING));
        if (this == STARTER) return modules;
        modules.addAll(EnumSet.of(
                CompanyModule.INBOX, CompanyModule.FOLLOW_UPS, CompanyModule.CAMPAIGNS,
                CompanyModule.ROLES, CompanyModule.AUDIT, CompanyModule.WHATSAPP));
        if (this == PROFESSIONAL) return modules;
        modules.addAll(EnumSet.of(
                CompanyModule.WORKFLOWS, CompanyModule.ASSISTANT, CompanyModule.AI_AGENT, CompanyModule.TELEGRAM));
        if (this == BUSINESS) return modules;
        return EnumSet.allOf(CompanyModule.class);
    }

    public boolean canAccessAdvancedFeatures() {
        return this == PROFESSIONAL || this == BUSINESS || this == ENTERPRISE;
    }

    public boolean canAccessEnterpriseFeatures() {
        return this == ENTERPRISE;
    }
}
