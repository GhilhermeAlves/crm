package com.becommerce.auth.infrastructure.gateway;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCookieFactoryTest {

    private GatewayCookieFactory factory(boolean secure) {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setCookieName("crm_session");
        properties.setCsrfCookieName("XSRF-TOKEN");
        properties.setSecureCookie(secure);
        properties.setSessionTtl(Duration.ofHours(8));
        return new GatewayCookieFactory(properties);
    }

    @Test
    void shouldBuildHttpOnlySameSiteSecureCookie() {
        GatewayCookieFactory factory = factory(true);

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
        GatewayCookieFactory factory = factory(false);

        ResponseCookie cookie = factory.createSessionCookie("opaque-token");

        assertFalse(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    void shouldBuildCsrfCookieReadableByJavascript() {
        GatewayCookieFactory factory = factory(true);

        ResponseCookie cookie = factory.createCsrfCookie("csrf-token");

        assertEquals("XSRF-TOKEN", cookie.getName());
        assertEquals("csrf-token", cookie.getValue());
        assertFalse(cookie.isHttpOnly(), "cookie CSRF precisa ser legível pelo browser (JS lê e envia no header)");
        assertEquals("Lax", cookie.getSameSite());
        assertEquals(Duration.ofHours(8), cookie.getMaxAge());
    }

    @Test
    void shouldBuildExpiredSessionCookieWithZeroMaxAge() {
        GatewayCookieFactory factory = factory(true);

        ResponseCookie cookie = factory.createExpiredSessionCookie();

        assertEquals("crm_session", cookie.getName());
        assertEquals(Duration.ZERO, cookie.getMaxAge(), "cookie de logout deve expirar imediatamente");
        assertTrue(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getSameSite());
    }

    @Test
    void shouldReadSessionAndCsrfTokensFromCookies() {
        GatewayCookieFactory factory = factory(true);

        Cookie[] cookies = {
                new Cookie("crm_session", "session-abc"),
                new Cookie("XSRF-TOKEN", "csrf-xyz"),
                new Cookie("unrelated", "x")
        };

        assertEquals(Optional.of("session-abc"), factory.readSessionToken(cookies));
        assertEquals(Optional.of("csrf-xyz"), factory.readCsrfToken(cookies));
        assertTrue(factory.readSessionToken(null).isEmpty());
        assertTrue(factory.readCsrfToken(new Cookie[0]).isEmpty());
    }
}
