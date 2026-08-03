package com.becommerce.auth.infrastructure.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

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
    private Duration authorizationRequestTtl = Duration.ofMinutes(10);
    private Duration tokenExchangeTimeout = Duration.ofSeconds(10);
    private Duration clockSkew = Duration.ofSeconds(30);

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
}
