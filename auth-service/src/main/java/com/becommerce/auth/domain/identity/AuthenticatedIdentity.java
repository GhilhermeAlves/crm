package com.becommerce.auth.domain.identity;

/**
 * Identidade já autenticada extraída do JWT oficial do Keycloak. É a única
 * origem da identidade — o cliente nunca informa {@code userId}/{@code companyId}/
 * {@code roles}/{@code permissions}; tudo é derivado deste contexto autenticado.
 *
 * @param keycloakSub      claim {@code sub} do Keycloak (obrigatória na prática)
 * @param email            claim {@code email}
 * @param preferredUsername claim {@code preferred_username}
 * @param givenName        claim {@code given_name}
 * @param familyName       claim {@code family_name}
 * @param displayName      claim {@code name} (ou combinação given+family)
 * @param sessionId        claim {@code sid} (sessão OIDC do Keycloak, opcional)
 * @param provider         claim {@code identity_provider} (Sprint 7.2): ausente
 *                         no login direto no realm, {@code google} (ou outro
 *                         alias) no login via Identity Brokering
 */
public record AuthenticatedIdentity(
        String keycloakSub,
        String email,
        String preferredUsername,
        String givenName,
        String familyName,
        String displayName,
        String sessionId,
        String provider) {
}
