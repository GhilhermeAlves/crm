package com.becommerce.auth.domain.gateway;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Estado transitório de uma autorização OIDC em andamento (Sprint 6.1). Criado
 * em {@code /auth/authorize} e consumido em {@code /auth/callback} para:
 *
 * <ul>
 *   <li>validar o {@code state} (anti-CSRF / anti-replay — uso único);</li>
 *   <li>validar o {@code nonce} do ID token (anti-replay);</li>
 *   <li>manter o {@code codeVerifier} (PKCE S256) longe do browser; e</li>
 *   <li>preservar o alvo ({@code redirect}) permitido após o login.</li>
 * </ul>
 */
public final class OidcAuthorizationRequest {

    private final String state;
    private final String nonce;
    private final String codeVerifier;
    private final String redirectTarget;
    private final Instant expiresAt;
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    /**
     * Base pública (esquema + host) pela qual o browser alcança o gateway no
     * momento do authorize (ex.: {@code http://localhost:3000} no dev local).
     * Nula quando o redirect_uri é o fixo configurado (comportamento clássico).
     */
    private final String publicBaseUrl;

    public OidcAuthorizationRequest(String state, String nonce, String codeVerifier,
                                    String redirectTarget, Instant expiresAt) {
        this(state, nonce, codeVerifier, redirectTarget, expiresAt, null);
    }

    public OidcAuthorizationRequest(String state, String nonce, String codeVerifier,
                                    String redirectTarget, Instant expiresAt, String publicBaseUrl) {
        this.state = Objects.requireNonNull(state, "state");
        this.nonce = Objects.requireNonNull(nonce, "nonce");
        this.codeVerifier = Objects.requireNonNull(codeVerifier, "codeVerifier");
        this.redirectTarget = Objects.requireNonNull(redirectTarget, "redirectTarget");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * Marca a requisição como consumida de forma atômica. Somente a primeira
     * chamada retorna {@code true}; as demais retornam {@code false} (replay).
     */
    public boolean consume() {
        return consumed.compareAndSet(false, true);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public String getState() {
        return state;
    }

    public String getNonce() {
        return nonce;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getRedirectTarget() {
        return redirectTarget;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
