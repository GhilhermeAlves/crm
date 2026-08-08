package com.becommerce.auth.infrastructure.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuração do Access Gateway OIDC (prefixo {@code auth.gateway}). Todos os
 * endpoints e o client apontam para o Keycloak (realm CRM) e são fornecidos via
 * variáveis de ambiente ({@code AUTH_GATEWAY_*}).
 */
@ConfigurationProperties(prefix = "auth.gateway")
public class OidcGatewayProperties {

    private boolean enabled = true;
    private String issuerUri = "";
    private String authorizationEndpoint = "";
    private String tokenEndpoint = "";
    private String jwksUri = "";
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private List<String> allowedRedirectUris = List.of();
    private List<String> tokenAudiences = List.of();
    private String defaultRedirect = "/";
    private String scope = "openid profile email";
    private String cookieName = "crm_session";
    private boolean secureCookie = true;
    private Duration sessionTtl = Duration.ofHours(8);
    private Duration sessionIdleTimeout = Duration.ZERO;
    private String csrfCookieName = "XSRF-TOKEN";
    private String csrfHeaderName = "X-XSRF-TOKEN";
    private String appBaseUrl = "";
    private Duration authorizationRequestTtl = Duration.ofMinutes(10);
    private Duration tokenExchangeTimeout = Duration.ofSeconds(10);
    private Duration clockSkew = Duration.ofSeconds(30);
    private String sessionStore = "memory";
    private Duration sessionLockTtl = Duration.ofSeconds(30);
    private Duration sessionLockAcquireTimeout = Duration.ofSeconds(5);
    private String apiBackendUrl = "";
    private Duration apiConnectTimeout = Duration.ofSeconds(5);
    private Duration apiReadTimeout = Duration.ofSeconds(30);
    private boolean rateLimitEnabled = true;
    private Duration rateLimitWindow = Duration.ofSeconds(60);
    private int rateLimitAuthorize = 20;
    private int rateLimitCallback = 20;
    private int rateLimitRefresh = 30;
    private int rateLimitLogout = 20;
    private int rateLimitApi = 60;
    private int rateLimitLink = 10;
    /**
     * Vínculo pendente (Sprint 7.2, Caso B): cookie HttpOnly efêmero que guarda
     * a referência ao {@code PendingLink} criado no callback quando o e-mail de
     * uma identidade externa coincide com conta local. Curto (10 min) e uso único.
     */
    private String pendingLinkCookieName = "crm_pending_link";
    private Duration pendingLinkTtl = Duration.ofMinutes(10);
    /**
     * Provedores de identidade habilitados (Identity Brokering, Sprint 7.0).
     * Aliases suportados: {@code google}, {@code microsoft}, {@code apple},
     * {@code phone}. Vazio = nenhum provedor externo ativo (login local
     * Keycloak, fluxo atual). Quando o IdP é configurado no Keycloak, o alias
     * entra aqui e o gateway passa {@code kc_idp_hint} para a autorização.
     * Meta/Facebook está fora de escopo.
     */
    private Set<String> enabledProviders = new LinkedHashSet<>();

    public boolean isConfigured() {
        return StringUtils.hasText(clientId)
                && StringUtils.hasText(issuerUri)
                && StringUtils.hasText(authorizationEndpoint)
                && StringUtils.hasText(tokenEndpoint)
                && StringUtils.hasText(jwksUri)
                && StringUtils.hasText(redirectUri);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public List<String> getAllowedRedirectUris() {
        return allowedRedirectUris;
    }

    public void setAllowedRedirectUris(List<String> allowedRedirectUris) {
        this.allowedRedirectUris = allowedRedirectUris == null ? List.of() : allowedRedirectUris;
    }

    public List<String> getTokenAudiences() {
        return tokenAudiences;
    }

    public void setTokenAudiences(List<String> tokenAudiences) {
        this.tokenAudiences = tokenAudiences == null ? List.of() : tokenAudiences;
    }

    public String getDefaultRedirect() {
        return defaultRedirect;
    }

    public void setDefaultRedirect(String defaultRedirect) {
        this.defaultRedirect = defaultRedirect;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }

    public void setSecureCookie(boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public Duration getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }

    public void setSessionIdleTimeout(Duration sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }

    public String getCsrfCookieName() {
        return csrfCookieName;
    }

    public void setCsrfCookieName(String csrfCookieName) {
        this.csrfCookieName = csrfCookieName;
    }

    public String getCsrfHeaderName() {
        return csrfHeaderName;
    }

    public void setCsrfHeaderName(String csrfHeaderName) {
        this.csrfHeaderName = csrfHeaderName;
    }

    public String getAppBaseUrl() {
        return appBaseUrl;
    }

    public void setAppBaseUrl(String appBaseUrl) {
        this.appBaseUrl = appBaseUrl;
    }

    public Duration getAuthorizationRequestTtl() {
        return authorizationRequestTtl;
    }

    public void setAuthorizationRequestTtl(Duration authorizationRequestTtl) {
        this.authorizationRequestTtl = authorizationRequestTtl;
    }

    public Duration getTokenExchangeTimeout() {
        return tokenExchangeTimeout;
    }

    public void setTokenExchangeTimeout(Duration tokenExchangeTimeout) {
        this.tokenExchangeTimeout = tokenExchangeTimeout;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    /**
     * Implementação do {@code GatewaySessionStore}: {@code memory} (padrão) ou
     * {@code redis} (distribuído, Sprint 6.3).
     */
    public String getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(String sessionStore) {
        this.sessionStore = sessionStore;
    }

    /** TTL do lock distribuído por sessão (default 30s). */
    public Duration getSessionLockTtl() {
        return sessionLockTtl;
    }

    public void setSessionLockTtl(Duration sessionLockTtl) {
        this.sessionLockTtl = sessionLockTtl;
    }

    /** Tempo máximo de espera para adquirir o lock de sessão (default 5s). */
    public Duration getSessionLockAcquireTimeout() {
        return sessionLockAcquireTimeout;
    }

    public void setSessionLockAcquireTimeout(Duration sessionLockAcquireTimeout) {
        this.sessionLockAcquireTimeout = sessionLockAcquireTimeout;
    }

    /**
     * Base URL do crm-backend (ex.: {@code http://localhost:8081}) usada pelo
     * BFF relay ({@code /api/**}) para repassar as requisições com o access
     * token da sessão — o browser nunca detém o token.
     */
    public String getApiBackendUrl() {
        return apiBackendUrl;
    }

    public void setApiBackendUrl(String apiBackendUrl) {
        this.apiBackendUrl = apiBackendUrl;
    }

    /** Timeout de conexão do BFF relay ao backend (default 5s). */
    public Duration getApiConnectTimeout() {
        return apiConnectTimeout;
    }

    public void setApiConnectTimeout(Duration apiConnectTimeout) {
        this.apiConnectTimeout = apiConnectTimeout;
    }

    /** Timeout de leitura do BFF relay ao backend (default 30s). */
    public Duration getApiReadTimeout() {
        return apiReadTimeout;
    }

    public void setApiReadTimeout(Duration apiReadTimeout) {
        this.apiReadTimeout = apiReadTimeout;
    }

    /** Habilita o rate limiting distribuído do Gateway (Sprint 6.6). */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    /** Janela fixa do rate limiting (default 60s). */
    public Duration getRateLimitWindow() {
        return rateLimitWindow;
    }

    public void setRateLimitWindow(Duration rateLimitWindow) {
        this.rateLimitWindow = rateLimitWindow;
    }

    /** Limite por janela de {@code /auth/authorize} (por IP real). */
    public int getRateLimitAuthorize() {
        return rateLimitAuthorize;
    }

    public void setRateLimitAuthorize(int rateLimitAuthorize) {
        this.rateLimitAuthorize = rateLimitAuthorize;
    }

    /** Limite por janela de {@code /auth/callback} (por IP real). */
    public int getRateLimitCallback() {
        return rateLimitCallback;
    }

    public void setRateLimitCallback(int rateLimitCallback) {
        this.rateLimitCallback = rateLimitCallback;
    }

    /** Limite por janela de {@code /auth/refresh} (por sessão). */
    public int getRateLimitRefresh() {
        return rateLimitRefresh;
    }

    public void setRateLimitRefresh(int rateLimitRefresh) {
        this.rateLimitRefresh = rateLimitRefresh;
    }

    /** Limite por janela de {@code /auth/logout} (por sessão). */
    public int getRateLimitLogout() {
        return rateLimitLogout;
    }

    public void setRateLimitLogout(int rateLimitLogout) {
        this.rateLimitLogout = rateLimitLogout;
    }

    /**
     * Limite por janela do relay {@code /api/**} (Sprint 6.7): por usuário
     * autenticado ({@code userId} da sessão) com fallback para IP real. {@code 0}
     * desativa o rate limiting do relay sem afetar os demais endpoints.
     */
    public int getRateLimitApi() {
        return rateLimitApi;
    }

    public void setRateLimitApi(int rateLimitApi) {
        this.rateLimitApi = rateLimitApi;
    }

    public Set<String> getEnabledProviders() {
        return enabledProviders;
    }

    public void setEnabledProviders(Set<String> enabledProviders) {
        this.enabledProviders = enabledProviders == null ? new LinkedHashSet<>() : enabledProviders;
    }

    /**
     * Limite por janela de {@code POST /auth/link} (Sprint 7.2): protege a
     * tentativa de senha da conta local no vínculo pendente (por IP real).
     */
    public int getRateLimitLink() {
        return rateLimitLink;
    }

    public void setRateLimitLink(int rateLimitLink) {
        this.rateLimitLink = rateLimitLink;
    }

    public String getPendingLinkCookieName() {
        return pendingLinkCookieName;
    }

    public void setPendingLinkCookieName(String pendingLinkCookieName) {
        this.pendingLinkCookieName = pendingLinkCookieName;
    }

    public Duration getPendingLinkTtl() {
        return pendingLinkTtl;
    }

    public void setPendingLinkTtl(Duration pendingLinkTtl) {
        this.pendingLinkTtl = pendingLinkTtl;
    }
}
