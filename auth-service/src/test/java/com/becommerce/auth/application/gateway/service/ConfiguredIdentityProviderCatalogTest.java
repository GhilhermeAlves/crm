package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(List.of("google", "microsoft", "apple", "phone"),
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
        properties.setEnabledProviders(Set.of("google", "microsoft"));

        IdentityProviderCatalog.IdentityProviderInfo google = catalog.find("google").orElseThrow();
        IdentityProviderCatalog.IdentityProviderInfo apple = catalog.find("apple").orElseThrow();
        assertTrue(google.available());
        assertTrue(catalog.find("microsoft").orElseThrow().available());
        assertFalse(apple.available());
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownAlias() {
        assertTrue(catalog.find("facebook").isEmpty());
        assertTrue(catalog.find("linkedin").isEmpty());
    }

    @Test
    void shouldExposeHumanReadableLabels() {
        assertEquals("Google", catalog.find("google").orElseThrow().label());
        assertEquals("Microsoft", catalog.find("microsoft").orElseThrow().label());
        assertEquals("Apple", catalog.find("apple").orElseThrow().label());
        assertEquals("Telefone", catalog.find("phone").orElseThrow().label());
    }
}
