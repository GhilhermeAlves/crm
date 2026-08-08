package com.becommerce.auth.domain.identity;

import java.time.Instant;
import java.util.Optional;

/**
 * Comportamento imut�vel de uma identidade de provedor externo autenticada (Google) do Keycloak.
 * Presente como um subprojeto ({@code provider}) de um usuário CRM, mantendo a estrat�gia
 * segura de identificador principal ({@code keycloak_sub}).
 */
public record AuthenticatedProviderIdentity(
        String keycloakSub,
        String email,
        String provider,
        String displayName,
        String givenName,
        String familyName,
        boolean emailVerified,
        Instant issuedAt,
        String issuer,
        String clientId) {

    public AuthenticatedProviderIdentity {
        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new IllegalArgumentException("keycloakSub n�o pode ser nulo ou vazio");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email n�o pode ser nulo ou vazio");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider n�o pode ser nulo ou vazio");
        }
    }

    /**
     * Obt�m o identificador can�nico do provedor externo para vincula��o: é o {@code sub}
     * (do JWT do Keycloak), **nunca o e-mail**.
     */
    public String getProviderId() {
        return keycloakSub;
    }

    /**
     * Valida a propriedade cr�tica de seguran�a: e-mail verificado pelo provedor.
     */
    public void requireVerifiedEmail() {
        if (!emailVerified) {
            throw new SecurityException("E-mail n�o verificado pelo provedor de identidade: vincula��o/provisionamento negado.");
        }
    }

    /**
     * Cria uma c�pia com o campo {@code displayName} opcional atualizado.
     */
    public AuthenticatedProviderIdentity withDisplayName(String displayName) {
        return new AuthenticatedProviderIdentity(
                this.keycloakSub,
                this.email,
                this.provider,
                displayName != null ? displayName : this.displayName,
                this.givenName,
                this.familyName,
                this.emailVerified,
                this.issuedAt,
                this.issuer,
                this.clientId
        );
    }
}
