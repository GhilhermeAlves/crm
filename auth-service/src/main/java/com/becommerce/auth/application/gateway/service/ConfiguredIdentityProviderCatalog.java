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
     * Meta/Facebook <b>não</b> faz parte do escopo. A disponibilidade de cada
     * provedor vem de {@link OidcGatewayProperties#getEnabledProviders()}
     * — o alias só é selecionável na tela de login (e só recebe
     * {@code kc_idp_hint}) quando estiver habilitado, o que normalmente ocorre
     * após o IdP ser configurado no Keycloak com as credenciais reais do
     * provedor externo.
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
                        provider.alias(), provider.label(),
                        properties.getEnabledProviders().contains(provider.alias())))
                .toList();
    }

    @Override
    public Optional<IdentityProviderInfo> find(String alias) {
        return list().stream()
                .filter(provider -> provider.alias().equals(alias))
                .findFirst();
    }
}
