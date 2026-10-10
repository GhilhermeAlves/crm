package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredIdentityProviderCatalogTest {

    private OidcGatewayProperties properties;
    private ConfiguredIdentityProviderCatalog catalog;

    @BeforeEach
    void setUp() {
        properties = new OidcGatewayProperties();
        catalog = new ConfiguredIdentityProviderCatalog(properties);
    }

    @Test
    void shouldListSupportedProvidersInDisplayOrderWithoutMeta() {
        List<IdentityProviderCatalog.IdentityProviderInfo> providers = catalog.list();
        assertEquals(List.of("google"),
                providers.stream().map(IdentityProviderCatalog.IdentityProviderInfo::alias).toList());
        assertTrue(providers.stream().noneMatch(p -> p.alias().equals("facebook")),
                "Meta/Facebook is out of scope");
    }

    @Test
    void shouldMarkProvidersUnavailableByDefault() {
        assertTrue(catalog.list().stream().noneMatch(IdentityProviderCatalog.IdentityProviderInfo::available));
    }

    @Test
    void shouldMarkEnabledProvidersAsAvailable() {
        properties.setEnabledProviders(Set.of("google"));

        IdentityProviderCatalog.IdentityProviderInfo google = catalog.find("google").orElseThrow();
        assertTrue(google.available());
    }

    @Test
    void shouldExposeGoogleAsTheOnlyAvailableProviderInSprint71ProductionShape() {
        properties.setEnabledProviders(Set.of("google"));

        IdentityProviderCatalog.IdentityProviderInfo google = catalog.find("google").orElseThrow();
        assertTrue(google.available());
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownAlias() {
        assertTrue(catalog.find("facebook").isEmpty());
        assertTrue(catalog.find("linkedin").isEmpty());
        assertTrue(catalog.find("microsoft").isEmpty());
        assertTrue(catalog.find("apple").isEmpty());
        assertTrue(catalog.find("phone").isEmpty());
    }

    @Test
    void shouldExposeHumanReadableLabels() {
        assertEquals("Google", catalog.find("google").orElseThrow().label());
    }
}
