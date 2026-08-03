package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Access Gateway OIDC (Sprints 6.1/6.2).
 *
 * <ul>
 *   <li>{@code GET /auth/authorize} — inicia o fluxo: valida redirect, gera
 *       {@code state}/{@code nonce}/PKCE S256 e redireciona (302) para o
 *       Authorization Endpoint do Keycloak;</li>
 *   <li>{@code GET /auth/callback} — recebe {@code code}+{@code state} do
 *       Keycloak, troca o código no servidor, valida os tokens, decide CRM
 *       Access e, em caso positivo, cria a sessão de browser (cookie
 *       HttpOnly/SameSite/Secure + cookie CSRF) e redireciona para o alvo
 *       permitido;</li>
 *   <li>{@code GET /auth/logout} — invalida a sessão local (idempotente),
 *       limpa o cookie e redireciona para o {@code end_session_endpoint} do
 *       provedor;</li>
 *   <li>{@code POST /auth/refresh} — renova os tokens no servidor (protegido
 *       por CSRF cookie-to-header via {@code GatewayCsrfFilter}); nunca devolve
 *       tokens ao browser (resposta 204).</li>
 * </ul>
 */
@RestController
public class OidcGatewayController {

    private final GatewayOidcUseCase gatewayOidcUseCase;
    private final GatewayCookieFactory cookieFactory;

    public OidcGatewayController(GatewayOidcUseCase gatewayOidcUseCase,
                                 GatewayCookieFactory cookieFactory) {
        this.gatewayOidcUseCase = gatewayOidcUseCase;
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/auth/authorize")
    public ResponseEntity<Void> authorize(@RequestParam(value = "redirect", required = false) String redirect) {
        GatewayOidcUseCase.BeginAuthorization result = gatewayOidcUseCase.beginAuthorization(redirect);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.authorizationUri()))
                .build();
    }

    @GetMapping("/auth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state) {
        if (StringUtils.hasText(error)) {
            throw new OidcGatewayException("OIDC_ERROR", HttpStatus.BAD_REQUEST.value(), errorMessage(error));
        }

        GatewayOidcUseCase.AuthenticationResult result = gatewayOidcUseCase.completeAuthorization(code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.redirectTarget()))
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createSessionCookie(result.session().sessionToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createCsrfCookie(result.session().csrfToken()).toString())
                .build();
    }

    @GetMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            @RequestParam(value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri) {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        GatewayOidcUseCase.LogoutResult result = gatewayOidcUseCase.logout(sessionToken, postLogoutRedirectUri);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.redirectUri()))
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpiredSessionCookie().toString())
                .build();
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        gatewayOidcUseCase.refresh(sessionToken);
        return ResponseEntity.noContent().build();
    }

    private String errorMessage(String error) {
        return switch (error) {
            case "access_denied" -> "Acesso negado pelo provedor de identidade.";
            case "login_cancelled", "login_required" -> "Login cancelado ou não concluído.";
            default -> "Falha na autenticação pelo provedor de identidade.";
        };
    }
}
