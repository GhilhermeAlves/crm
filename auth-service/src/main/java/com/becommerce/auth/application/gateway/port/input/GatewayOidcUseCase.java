package com.becommerce.auth.application.gateway.port.input;

import com.becommerce.auth.domain.gateway.GatewaySession;

/**
 * Porta de entrada do Access Gateway OIDC (Sprint 6.1). Orquestra o fluxo
 * Authorization Code + PKCE S256 iniciado pelo Auth Service:
 *
 * <ul>
 *   <li>{@code beginAuthorization} — valida o redirect, gera {@code state}/
 *       {@code nonce}/PKCE e monta a URL de autorização do Keycloak;</li>
 *   <li>{@code completeAuthorization} — valida {@code state}, troca o código no
 *       servidor, valida os tokens, decide CRM Access e cria a sessão de
 *       browser (cookie HttpOnly).</li>
 * </ul>
 */
public interface GatewayOidcUseCase {

    BeginAuthorization beginAuthorization(String redirect);

    AuthenticationResult completeAuthorization(String code, String state);

    record BeginAuthorization(String authorizationUri, String redirectTarget) {
    }

    record AuthenticationResult(GatewaySession session, String redirectTarget) {
    }
}
