package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.infrastructure.gateway.ForwardedOriginResolver;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final IdentityProviderCatalog identityProviderCatalog;
    private final OidcGatewayProperties properties;
    private final ForwardedOriginResolver forwardedOriginResolver;

    public OidcGatewayController(GatewayOidcUseCase gatewayOidcUseCase,
                                 GatewayCookieFactory cookieFactory,
                                 IdentityProviderCatalog identityProviderCatalog,
                                 OidcGatewayProperties properties,
                                 ForwardedOriginResolver forwardedOriginResolver) {
        this.gatewayOidcUseCase = gatewayOidcUseCase;
        this.cookieFactory = cookieFactory;
        this.identityProviderCatalog = identityProviderCatalog;
        this.properties = properties;
        this.forwardedOriginResolver = forwardedOriginResolver;
    }

    @GetMapping("/auth/providers")
    public List<IdentityProviderCatalog.IdentityProviderInfo> providers() {
        return identityProviderCatalog.list();
    }

    @GetMapping("/auth/authorize")
    public ResponseEntity<Void> authorize(
            HttpServletRequest request,
            @RequestParam(value = "redirect", required = false) String redirect,
            @RequestParam(value = "provider", required = false) String provider) {
        GatewayOidcUseCase.BeginAuthorization result =
                gatewayOidcUseCase.beginAuthorization(redirect, provider, publicOrigin(request));
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

        if (result.pendingLink() != null) {
            // Caso B (Sprint 7.2): segue para /link-account com o vínculo
            // pendente. Cookies antigos de sessão são limpos (estado transitório);
            // o token CSRF novo habilita o POST /auth/link (cookie-to-header).
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(result.redirectTarget()))
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.createPendingLinkCookie(result.pendingLink().token()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.createCsrfCookie(result.pendingLink().csrfToken()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpiredSessionCookie().toString())
                    .build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.redirectTarget()))
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createSessionCookie(result.session().sessionToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createCsrfCookie(result.session().csrfToken()).toString())
                .build();
    }

    /**
     * Sprint 7.2 (Caso B): estado do vínculo pendente. Público; autenticado pelo
     * cookie HttpOnly {@code crm_pending_link}. Retorna {@code pending=false}
     * quando ausente/expirado e o e-mail apenas para o usuário identificar a
     * conta local no fluxo.
     */
    @GetMapping("/auth/link-status")
    public Map<String, Object> linkStatus(HttpServletRequest request) {
        String pendingToken = cookieFactory.readPendingLinkToken(request.getCookies()).orElse(null);
        GatewayOidcUseCase.LinkStatusResult result = gatewayOidcUseCase.linkStatus(pendingToken);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pending", result.pending());
        if (result.email() != null) {
            body.put("email", result.email());
        }
        return body;
    }

    /**
     * Sprint 7.2 (Caso B): conclui o vínculo pendente verificando a senha da
     * conta local no crm-backend. Sucesso → sessão real + redirect original
     * (o browser só recebe o redirect, nunca tokens). Protegido por CSRF
     * cookie-to-header ({@code GatewayCsrfFilter}) e rate limit.
     */
    @PostMapping("/auth/link")
    public ResponseEntity<Map<String, Object>> link(HttpServletRequest request, @RequestBody LinkAccountRequest body) {
        Objects.requireNonNull(body, "body");
        String pendingToken = cookieFactory.readPendingLinkToken(request.getCookies()).orElse(null);
        GatewayOidcUseCase.LinkResult result = gatewayOidcUseCase.completeLink(pendingToken, body.password());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("redirect", result.redirectTarget());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createSessionCookie(result.session().sessionToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createCsrfCookie(result.session().csrfToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpiredPendingLinkCookie().toString())
                .body(response);
    }

    private record LinkAccountRequest(String password) {
        private LinkAccountRequest {
            Objects.requireNonNull(password, "password");
        }
    }

    @GetMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            @RequestParam(value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri) {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        GatewayOidcUseCase.LogoutResult result =
                gatewayOidcUseCase.logout(sessionToken, postLogoutRedirectUri, publicOrigin(request));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.redirectUri()))
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpiredSessionCookie().toString())
                .build();
    }

    /**
     * Origem pública derivada do request, apenas quando o modo dinâmico está
     * habilitado (null caso contrário — comportamento fixo clássico).
     */
    private String publicOrigin(HttpServletRequest request) {
        if (!properties.isDynamicRedirectUri()) {
            return null;
        }
        return forwardedOriginResolver.resolve(request);
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
