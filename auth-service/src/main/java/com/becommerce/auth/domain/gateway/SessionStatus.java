package com.becommerce.auth.domain.gateway;

/**
 * Status de uma sessão de gateway resolvida a partir do {@code sessionToken}
 * do cookie (Sprint 6.2). Permite aos fluxos de logout/refresh distinguir uma
 * sessão inexistente de uma expirada ou revogada.
 */
public enum SessionStatus {

    /** Sessão válida e dentro da expiração efetiva. */
    ACTIVE,

    /** Sessão vencida por TTL absoluto ou idle timeout. */
    EXPIRED,

    /** Sessão revogada (tombstone — ex.: logout ou refresh falho). */
    REVOKED,

    /** Nenhuma sessão conhecida para o token informado. */
    NOT_FOUND
}
