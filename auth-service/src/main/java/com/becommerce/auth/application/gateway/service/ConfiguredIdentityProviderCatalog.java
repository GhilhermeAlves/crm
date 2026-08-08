package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;

import java.util.List;
import java.util.Optional;

    /**
     * Catálogo de provedores de identidade configurado ({@code auth.gateway}).
     *
     * <p>O registro de provedores suportados é fixo (Sprint 7.0): Google e
     * Telefone/OTP (Microsoft/Apple removidos por estarem fora do escopo).
     * Meta/Facebook <b>não</b> faz parte do escopo.
     *
     * <p>Duas fontes de disponibilidade distintas (Sprint 7.4):
     * <ul>
     *   <li><b>Google</b> — Identity Provider do Keycloak (Identity Brokering):
     *       disponível quando o alias está em {@code enabled-providers}
     *       ({@link OidcGatewayProperties#getEnabledProviders()}), o que
     *       normalmente ocorre após o IdP ser configurado no Keycloak. O clique
     *       gera {@code kc_idp_hint} na autorização;</li>
     *   <li><b>Telefone</b> — provedor local de OTP ({@code phone-enabled}):
     *       NÃO existe como IdP no Keycloak; a tela de login coleta o OTP e,
     *       após confirmar, segue para o fluxo de senha do Keycloak. Nunca
     *       recebe {@code kc_idp_hint} (ver guard em
     *       {@code GatewayOidcService#applyIdentityProviderHint}).</li>
     * </ul>
     */
public class ConfiguredIdentityProviderCatalog implements IdentityProviderCatalog {

    private static final List<IdentityProviderInfo> REGISTRY = List.of(
            new IdentityProviderInfo("google", "Google", false),
            new IdentityProviderInfo("phone", "Telefone", false));

    private final OidcGatewayProperties properties;

    public ConfiguredIdentityProviderCatalog(OidcGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<IdentityProviderInfo> list() {
        return REGISTRY.stream()
                .map(provider -> new IdentityProviderInfo(
                        provider.alias(), provider.label(), isAvailable(provider.alias())))
                .toList();
    }

    @Override
    public Optional<IdentityProviderInfo> find(String alias) {
        return list().stream()
                .filter(provider -> provider.alias().equals(alias))
                .findFirst();
    }

    /**
     * Disponibilidade por origem: Google via {@code enabled-providers} (IdP do
     * Keycloak); Telefone via {@code phone-enabled} (provedor local de OTP).
     */
    private boolean isAvailable(String alias) {
        if ("phone".equals(alias)) {
            return properties.isPhoneEnabled();
        }
        return properties.getEnabledProviders().contains(alias);
    }
}
