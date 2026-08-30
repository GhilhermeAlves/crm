package com.becommerce.auth.infrastructure.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ForwardedOriginResolverTest {

    private final ForwardedOriginResolver resolver = new ForwardedOriginResolver();

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    @Test
    void shouldDeriveOriginFromHostWhenNoForwardedHeaders() {
        MockHttpServletRequest req = request();
        req.setServerName("srv1348261.hstgr.cloud");
        req.setScheme("https");
        req.setServerPort(443);

        assertEquals("https://srv1348261.hstgr.cloud", resolver.resolve(req));
    }

    @Test
    void shouldDeriveLocalhostOriginWithServerPort() {
        MockHttpServletRequest req = request();
        req.setServerName("localhost");
        req.setScheme("http");
        req.setServerPort(3000);

        assertEquals("http://localhost:3000", resolver.resolve(req),
                "sem X-Forwarded-Host o getServerPort() deve compor a porta na origem");
    }

    @Test
    void shouldNotAppendDefaultPortForLocalhost() {
        MockHttpServletRequest req = request();
        req.setServerName("localhost");
        req.setServerPort(80);

        assertEquals("http://localhost", resolver.resolve(req));
    }

    @Test
    void shouldUseForwardedHostOverServerName() {
        MockHttpServletRequest req = request();
        req.setServerName("srv1348261.hstgr.cloud");
        req.setScheme("https");
        req.addHeader("X-Forwarded-Host", "localhost:3000");

        assertEquals("http://localhost:3000", resolver.resolve(req), "localhost deve forçar http");
    }

    @Test
    void shouldForceHttpOnlyForLocalhostEvenWhenForwardedProtoIsHttps() {
        MockHttpServletRequest req = request();
        req.setServerName("srv1348261.hstgr.cloud");
        req.addHeader("X-Forwarded-Host", "localhost:3000");
        req.addHeader("X-Forwarded-Proto", "https");

        assertEquals("http://localhost:3000", resolver.resolve(req),
                "nginx sobrescreve X-Forwarded-Proto; localhost deve ser http por construção");
    }

    @Test
    void shouldKeepPortInHost() {
        MockHttpServletRequest req = request();
        req.setServerName("crm.hstgr.cloud");
        req.setScheme("https");
        req.addHeader("X-Forwarded-Host", "app.example.com:8443");

        assertEquals("https://app.example.com:8443", resolver.resolve(req));
    }

    @Test
    void shouldUseForwardedProtoForNonLocalhost() {
        MockHttpServletRequest req = request();
        req.setServerName("crm.hstgr.cloud");
        req.setScheme("http");
        req.setServerPort(443);
        req.addHeader("X-Forwarded-Proto", "https");

        assertEquals("https://crm.hstgr.cloud", resolver.resolve(req));
    }

    @Test
    void shouldReturnNullWhenNoHostAtAll() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServerName("");

        assertNull(resolver.resolve(req));
    }

    @Test
    void shouldReturnNullWhenForwardedHostIsBlank() {
        MockHttpServletRequest req = request();
        req.addHeader("X-Forwarded-Host", "   ");
        req.setServerName("srv1348261.hstgr.cloud");
        req.setServerPort(443);

        assertEquals("https://srv1348261.hstgr.cloud", resolver.resolve(req));
    }
}