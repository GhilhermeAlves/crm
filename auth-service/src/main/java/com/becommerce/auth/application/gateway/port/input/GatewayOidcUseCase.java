package com.becommerce.auth.application.gateway.port.input;

import com.becommerce.auth.domain.gateway.GatewaySession;

/**
 * Porta de entrada do Access Gateway OIDC (Sprints 6.1/6.2). Orquestra o fluxo
 * Authorization Code + PKCE S256 iniciado pelo Auth Service:
 *
 * <ul>
 *   <li>{@code beginAuthorization} — valida o redirect, gera {@code state}/
 *       {@code nonce}/PKCE e monta a URL de autorização do Keycloak;</li>
 *   <li>{@code completeAuthorization} — valida {@code state}, troca o código no
 *       servidor, valida os tokens, decide CRM Access e cria a sessão de
 *       browser (cookie HttpOnly);</li>
 *   <li>{@code logout} — invalida a sessão local (idempotente) e monta o
 *       redirect para o {@code end_session_endpoint} do provedor;</li>
 *   <li>{@code refresh} — renova os tokens no servidor (rotação, lock por
 *       sessão) sem devolver tokens ao browser.</li>
 * </ul>
 */
public interface GatewayOidcUseCase {

    BeginAuthorization beginAuthorization(String redirect);

    AuthenticationResult completeAuthorization(String code, String state);

    LogoutResult logout(String sessionToken, String postLogoutRedirectUri);

    RefreshResult refresh(String sessionToken);

    record BeginAuthorization(String authorizationUri, String redirectTarget) {
    }

    record AuthenticationResult(GatewaySession session, String redirectTarget) {
    }

    record LogoutResult(String redirectUri) {
    }

    record RefreshResult(GatewaySession session) {
    }
}
