package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedirectUriValidatorTest {

    private OidcGatewayProperties properties;
    private RedirectUriValidator validator;

    @BeforeEach
    void setUp() {
        properties = new OidcGatewayProperties();
        properties.setAllowedRedirectUris(List.of(
                "http://localhost:3000",
                "https://srv1348261.hstgr.cloud",
                "https://app.example.com/app/*"));
        properties.setDefaultRedirect("/");
        validator = new RedirectUriValidator(properties);
    }

    @Test
    void shouldAcceptRelativePathOfSameOrigin() {
        assertEquals("/dashboard", validator.validateAndNormalize("/dashboard"));
        assertEquals("/", validator.validateAndNormalize("/"));
    }

    @Test
    void shouldUseDefaultRedirectWhenBlank() {
        assertEquals("/", validator.validateAndNormalize(null));
        assertEquals("/", validator.validateAndNormalize("  "));
    }

    @Test
    void shouldAcceptAbsoluteUriInAllowlist() {
        assertEquals("http://localhost:3000/dashboard",
                validator.validateAndNormalize("http://localhost:3000/dashboard"));
        assertEquals("https://srv1348261.hstgr.cloud/app",
                validator.validateAndNormalize("https://srv1348261.hstgr.cloud/app"));
    }

    @Test
    void shouldAcceptHttpLocalhostOnlyWithExplicitAllowlistEntry() {
        assertEquals("http://localhost:3000/x", validator.validateAndNormalize("http://localhost:3000/x"));
    }

    @Test
    void shouldRejectOpenRedirectProtocolRelative() {
        assertThrows(OidcGatewayException.class, () -> validator.validateAndNormalize("//evil.example"));
    }

    @Test
    void shouldRejectCrossOriginRedirect() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> validator.validateAndNormalize("https://evil.example/phish"));
        assertEquals("OPEN_REDIRECT", ex.getCode());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void shouldRejectHttpNonLocalhostEvenInsideOtherwiseValidOrigin() {
        properties.setAllowedRedirectUris(List.of("http://evil.example"));
        assertThrows(OidcGatewayException.class, () -> validator.validateAndNormalize("http://evil.example/x"));
    }

    @Test
    void shouldRejectMalformedUri() {
        assertThrows(OidcGatewayException.class, () -> validator.validateAndNormalize("http://[invalid"));
    }

    @Test
    void shouldHonorPathPrefixAllowlistEntry() {
        assertEquals("https://app.example.com/app/panel",
                validator.validateAndNormalize("https://app.example.com/app/panel"));
        assertThrows(OidcGatewayException.class, () -> validator.validateAndNormalize("https://app.example.com/other"));
    }

    @Test
    void shouldRejectEmptyNonSlashRelative() {
        assertThrows(OidcGatewayException.class, () -> validator.validateAndNormalize("dashboard"));
    }
}
