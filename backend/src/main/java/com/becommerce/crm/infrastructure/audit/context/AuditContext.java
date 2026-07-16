package com.becommerce.crm.infrastructure.audit.context;

import java.util.UUID;

public class AuditContext {

    private static final ThreadLocal<AuditContextData> CONTEXT = new ThreadLocal<>();

    public static void set(UUID userId, String userName, String userEmail, UUID companyId,
                           String ipAddress, String userAgent) {
        CONTEXT.set(new AuditContextData(userId, userName, userEmail, companyId, ipAddress, userAgent));
    }

    public static AuditContextData get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record AuditContextData(
        UUID userId,
        String userName,
        String userEmail,
        UUID companyId,
        String ipAddress,
        String userAgent
    ) {}
}
