package com.becommerce.auth.domain.company;

/**
 * Status da empresa no banco CRM compartilhado (tabela {@code companies},
 * coluna {@code status}). Somente {@code ACTIVE} permite operar o CRM.
 */
public enum CompanyStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    ONBOARDING;

    public boolean canOperate() {
        return this == ACTIVE;
    }
}
