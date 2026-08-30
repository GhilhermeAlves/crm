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

    /**
     * Inicia o fluxo de autorização. {@code provider} (opcional) é o alias de
     * um Identity Provider do Keycloak (Identity Brokering, Sprint 7.0): quando
     * presente e disponível, o gateway adiciona {@code kc_idp_hint} à URL de
     * autorização para encaminhar o usuário direto ao provedor. Nenhum token de
     * provedor externo transita pelo browser.
     */
    BeginAuthorization beginAuthorization(String redirect, String provider);

    /**
     * Overload sem provider — preserva o fluxo atual (login local Keycloak).
     */
    default BeginAuthorization beginAuthorization(String redirect) {
        return beginAuthorization(redirect, null);
    }

    /**
     * Overload com origem pública derivada do request (dev local / múltiplos
     * hostnames). {@code publicOrigin} (ex.: {@code http://localhost:3000}) é
     * aceito apenas quando o modo dinâmico está habilitado E a origem está na
     * allowlist; caso contrário o serviço usa o {@code redirect_uri} fixo.
     */
    BeginAuthorization beginAuthorization(String redirect, String provider, String publicOrigin);

    AuthenticationResult completeAuthorization(String code, String state);

    /**
     * Overload que aceita a origem pública do request para resolver a base do
     * {@code post_logout_redirect_uri} (mesmo critério do authorize).
     */
    LogoutResult logout(String sessionToken, String postLogoutRedirectUri, String publicOrigin);

    /**
     * Overload sem origem pública — preserva o fluxo atual.
     */
    default LogoutResult logout(String sessionToken, String postLogoutRedirectUri) {
        return logout(sessionToken, postLogoutRedirectUri, null);
    }

    RefreshResult refresh(String sessionToken);

    /**
     * Estado do vínculo pendente (Sprint 7.2, Caso B): {@code true} quando o
     * cookie {@code crm_pending_link} referencia um vínculo válido, expondo
     * apenas o e-mail (para o usuário identificar a conta local).
     */
    LinkStatusResult linkStatus(String pendingToken);

    /**
     * Conclui o vínculo pendente (Caso B): verifica a senha da conta local no
     * crm-backend ({@code POST /internal/auth/link}) e, em caso positivo, cria
     * a sessão de browser real e retorna o redirect original. Erros:
     * {@code INVALID_CREDENTIALS} (401), {@code LINK_PENDING_NOT_FOUND} (410),
     * {@code LINK_NOT_FOUND} (410).
     */
    LinkResult completeLink(String pendingToken, String password);

    record BeginAuthorization(String authorizationUri, String redirectTarget) {
    }

    /**
     * Resultado do callback: exatamente um dos dois — sessão criada
     * ({@code session}) ou vínculo pendente iniciado ({@code pendingLink}).
     * Para vínculo pendente, {@code redirectTarget} aponta para a página
     * {@code /link-account} do frontend.
     */
    record AuthenticationResult(GatewaySession session, PendingLinkInfo pendingLink, String redirectTarget) {
    }

    record PendingLinkInfo(String token, String email, String csrfToken, String redirectTarget) {
    }

    record LogoutResult(String redirectUri) {
    }

    record RefreshResult(GatewaySession session) {
    }

    record LinkStatusResult(boolean pending, String email) {
    }

    record LinkResult(String redirectTarget, GatewaySession session) {
    }
}
