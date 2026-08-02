package com.becommerce.auth.infrastructure.tenant.context;

import java.util.UUID;

/**
 * Contexto de tenant e de bootstrap de identidade por requisição, espelhado no
 * crm-backend. O {@code TenantAwareDataSource} aplica os valores como GUCs
 * ({@code app.current_company_id} / {@code app.current_keycloak_sub}) a cada
 * conexão obtida, para que o RLS FORCE do banco CRM funcione também com o
 * usuário {@code crm_app} (NOBYPASSRLS).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMPANY = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_KEYCLOAK_SUB = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCompanyId(UUID companyId) {
        CURRENT_COMPANY.set(companyId);
    }

    public static UUID getCompanyId() {
        return CURRENT_COMPANY.get();
    }

    public static void setKeycloakSub(String keycloakSub) {
        CURRENT_KEYCLOAK_SUB.set(keycloakSub);
    }

    public static String getKeycloakSub() {
        return CURRENT_KEYCLOAK_SUB.get();
    }

    public static void clear() {
        CURRENT_COMPANY.remove();
        CURRENT_KEYCLOAK_SUB.remove();
    }
}
