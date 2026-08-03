package com.becommerce.auth.infrastructure.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCookieFactoryTest {

    @Test
    void shouldBuildHttpOnlySameSiteSecureCookie() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setCookieName("crm_session");
        properties.setSecureCookie(true);
        properties.setSessionTtl(Duration.ofHours(8));
        GatewayCookieFactory factory = new GatewayCookieFactory(properties);

        ResponseCookie cookie = factory.createSessionCookie("opaque-token");

        assertEquals("crm_session", cookie.getName());
        assertEquals("opaque-token", cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals(Duration.ofHours(8), cookie.getMaxAge());
    }

    @Test
    void shouldNotSetSecureFlagWhenDisabled() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setSecureCookie(false);
        GatewayCookieFactory factory = new GatewayCookieFactory(properties);

        ResponseCookie cookie = factory.createSessionCookie("opaque-token");

        assertFalse(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
