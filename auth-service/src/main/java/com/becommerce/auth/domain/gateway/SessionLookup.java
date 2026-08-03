package com.becommerce.auth.domain.gateway;

/**
 * Resultado da resolução de uma sessão de gateway (Sprint 6.2).
 * {@code session} está presente somente quando {@code status == ACTIVE}.
 */
public record SessionLookup(SessionStatus status, GatewaySession session) {

    public static SessionLookup active(GatewaySession session) {
        return new SessionLookup(SessionStatus.ACTIVE, session);
    }

    public static SessionLookup expired(GatewaySession session) {
        return new SessionLookup(SessionStatus.EXPIRED, session);
    }

    public static SessionLookup revoked(GatewaySession session) {
        return new SessionLookup(SessionStatus.REVOKED, session);
    }

    public static SessionLookup notFound() {
        return new SessionLookup(SessionStatus.NOT_FOUND, null);
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }
}
