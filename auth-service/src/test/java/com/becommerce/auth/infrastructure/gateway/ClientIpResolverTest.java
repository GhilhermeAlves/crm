package com.becommerce.auth.infrastructure.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes de resolução do IP real do cliente (Sprint 6.7, seção de segurança):
 * a cadeia real usa {@code X-Real-IP $remote_addr} (sobrescrito pelo proxy) e
 * {@code X-Forwarded-For $proxy_add_x_forwarded_for} (REMOTE_ADDR anexado ao
 * final). O primeiro valor do XFF é controlável pelo cliente e não pode ser
 * aceito.
 */
class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/v1/users");
    }

    @Test
    void shouldPreferTrustedXRealIpOverForgeableForwardedFor() {
        MockHttpServletRequest request = request();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "189.60.1.2");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 189.60.1.2");

        assertEquals("189.60.1.2", resolver.resolve(request));
    }

    @Test
    void shouldUseLastPlausibleForwardedForWhenNoXRealIp() {
        MockHttpServletRequest request = request();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 189.60.1.2");

        assertEquals("189.60.1.2", resolver.resolve(request));
    }

    @Test
    void shouldIgnoreForgedFirstForwardedForWithProxyAddedChain() {
        MockHttpServletRequest request = request();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 189.60.1.2");

        assertEquals("189.60.1.2", resolver.resolve(request));
    }

    @Test
    void shouldFallbackToRemoteAddrWhenNoHeaders() {
        MockHttpServletRequest request = request();
        request.setRemoteAddr("10.1.2.3");

        assertEquals("10.1.2.3", resolver.resolve(request));
    }

    @Test
    void shouldIgnoreMalformedForwardedForEntries() {
        MockHttpServletRequest request = request();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "not-an-ip, 189.60.1.2");

        assertEquals("189.60.1.2", resolver.resolve(request));
    }
}
