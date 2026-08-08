package com.becommerce.auth.infrastructure.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Configuração do acesso de ADMIN ao Keycloak (prefixo {@code auth.keycloak.admin}).
 * Usado pelo reset de credencial (Sprint 7.4): o auth-service obtém um token de
 * service account via {@code client_credentials} de um client confidencial de
 * ADMIN (ex.: {@code crm-keycloak-admin}) e chama o Admin REST do realm. As
 * credenciais chegam via variáveis de ambiente ({@code AUTH_KEYCLOAK_ADMIN_*}).
 */
@ConfigurationProperties(prefix = "auth.keycloak.admin")
public class KeycloakAdminProperties {

    private String baseUrl = "http://crm-keycloak:8080";
    private String realm = "CRM";
    private String clientId = "";
    private String clientSecret = "";
    /** Timeout do token endpoint de service account (default 10s). */
    private Duration tokenTimeout = Duration.ofSeconds(10);
    /** Timeout das chamadas ao Admin REST do Keycloak (default 15s). */
    private Duration adminTimeout = Duration.ofSeconds(15);

    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl)
                && StringUtils.hasText(realm)
                && StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public Duration getTokenTimeout() {
        return tokenTimeout;
    }

    public void setTokenTimeout(Duration tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    public Duration getAdminTimeout() {
        return adminTimeout;
    }

    public void setAdminTimeout(Duration adminTimeout) {
        this.adminTimeout = adminTimeout;
    }
}