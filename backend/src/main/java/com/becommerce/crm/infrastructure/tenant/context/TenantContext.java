package com.becommerce.crm.infrastructure.tenant.context;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMPANY = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_KEYCLOAK_SUB = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_IDENTITY_EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_IDENTITY_PHONE = new ThreadLocal<>();

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

    /**
     * E-mail da identidade autenticada (claim {@code email} do JWT), usado no
     * bootstrap de identidade por e-mail (V024): permite ler e vincular a
     * PRÓPRIA linha em {@code users} via {@code app.current_identity_email}
     * antes de o {@code company_id} ser conhecido (RLS FORCE). Definido pela
     * resolução de {@code CurrentUser} e pelos endpoints internos de identidade.
     */
    public static void setIdentityEmail(String identityEmail) {
        CURRENT_IDENTITY_EMAIL.set(identityEmail);
    }

    public static String getIdentityEmail() {
        return CURRENT_IDENTITY_EMAIL.get();
    }

    public static boolean hasIdentityEmail() {
        return CURRENT_IDENTITY_EMAIL.get() != null && !CURRENT_IDENTITY_EMAIL.get().isBlank();
    }

    /**
     * Telefone em E.164 cuja POSSE foi provada por OTP (Sprint 7.3), usado no
     * bootstrap de identidade por telefone: permite ler e vincular a PRÓPRIA
     * linha em {@code users} via {@code app.current_identity_phone} sob RLS
     * FORCE (durante o verify-otp anônimo, antes de o {@code company_id} ser
     * conhecido). O GUC só é definido após a validação do OTP (prova de posse).
     */
    public static void setIdentityPhone(String identityPhone) {
        CURRENT_IDENTITY_PHONE.set(identityPhone);
    }

    public static String getIdentityPhone() {
        return CURRENT_IDENTITY_PHONE.get();
    }

    public static boolean hasIdentityPhone() {
        return CURRENT_IDENTITY_PHONE.get() != null && !CURRENT_IDENTITY_PHONE.get().isBlank();
    }

    /** Limpa apenas o telefone de identidade (sem afetar os demais contextos). */
    public static void clearIdentityPhone() {
        CURRENT_IDENTITY_PHONE.remove();
    }

    public static void clear() {
        CURRENT_COMPANY.remove();
        CURRENT_KEYCLOAK_SUB.remove();
        CURRENT_IDENTITY_EMAIL.remove();
        CURRENT_IDENTITY_PHONE.remove();
    }
}
