package com.becommerce.crm.infrastructure.tenant.context;

import java.util.UUID;

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

    public static boolean hasCompanyId() {
        return CURRENT_COMPANY.get() != null;
    }

    /**
     * Sub do usuário autenticado no Keycloak, usado no bootstrap de identidade:
     * permite ler a própria linha em {@code users} via
     * {@code app.current_keycloak_sub} ANTES de o {@code company_id} ser conhecido
     * (RLS FORCE). Definido pelo resolver de {@code CurrentUser}.
     */
    public static void setKeycloakSub(String keycloakSub) {
        CURRENT_KEYCLOAK_SUB.set(keycloakSub);
    }

    public static String getKeycloakSub() {
        return CURRENT_KEYCLOAK_SUB.get();
    }

    public static boolean hasKeycloakSub() {
        return CURRENT_KEYCLOAK_SUB.get() != null && !CURRENT_KEYCLOAK_SUB.get().isBlank();
    }

    public static void clear() {
        CURRENT_COMPANY.remove();
        CURRENT_KEYCLOAK_SUB.remove();
    }
}
