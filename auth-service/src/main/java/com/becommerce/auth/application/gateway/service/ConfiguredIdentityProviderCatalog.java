package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;

import java.util.List;
import java.util.Optional;

    /**
     * Catálogo de provedores de identidade configurado ({@code auth.gateway}).
     *
     * <p>O registro de provedores suportados é fixo: apenas Google, um Identity
     * Provider do Keycloak (Identity Brokering), disponível quando o alias está em
     * {@code enabled-providers} ({@link OidcGatewayProperties#getEnabledProviders()}).
     * O clique gera {@code kc_idp_hint} na autorização.
     */
public class ConfiguredIdentityProviderCatalog implements IdentityProviderCatalog {

    private static final List<IdentityProviderInfo> REGISTRY = List.of(
            new IdentityProviderInfo("google", "Google", false));

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

    private boolean isAvailable(String alias) {
        return properties.getEnabledProviders().contains(alias);
    }
}
