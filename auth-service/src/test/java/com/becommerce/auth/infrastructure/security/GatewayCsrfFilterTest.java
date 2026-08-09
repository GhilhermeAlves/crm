package com.becommerce.auth.infrastructure.security;

import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCsrfFilterTest {

    private GatewayCsrfFilter filter;

    @BeforeEach
    void setUp() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setCookieName("crm_session");
        properties.setCsrfCookieName("XSRF-TOKEN");
        properties.setCsrfHeaderName("X-XSRF-TOKEN");
        properties.setSecureCookie(true);
        filter = new GatewayCsrfFilter(new GatewayCookieFactory(properties), properties, new ObjectMapper());
    }

    private MockHttpServletRequest post(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        return request;
    }

    private Cookie csrfCookie(String value) {
        return new Cookie("XSRF-TOKEN", value);
    }

    @Test
    void shouldPassWhenCookieAndHeaderMatch() throws Exception {
        MockHttpServletRequest request = post("/auth/refresh");
        request.setCookies(csrfCookie("csrf-123"));
        request.addHeader("X-XSRF-TOKEN", "csrf-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "chain deve prosseguir quando cookie e header coincidem");
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = post("/auth/refresh");
        request.setCookies(csrfCookie("csrf-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertRejected(chain, response);
    }

    @Test
    void shouldRejectWhenCookieMissing() throws Exception {
        MockHttpServletRequest request = post("/auth/refresh");
        request.addHeader("X-XSRF-TOKEN", "csrf-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertRejected(chain, response);
    }

    @Test
    void shouldRejectWhenTokensDiffer() throws Exception {
        MockHttpServletRequest request = post("/auth/refresh");
        request.setCookies(csrfCookie("csrf-cookie"));
        request.addHeader("X-XSRF-TOKEN", "csrf-other");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertRejected(chain, response);
    }

    @Test
    void shouldNotInterceptGetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/refresh");
        request.setRequestURI("/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "GET não deve ser interceptado pelo CSRF");
    }

    @Test
    void shouldNotInterceptOtherPaths() throws Exception {
        MockHttpServletRequest request = post("/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "somente /auth/refresh e /auth/link são protegidos por CSRF");
    }

    @Test
    void shouldProtectLinkEndpointWhenTokensMatch() throws Exception {
        MockHttpServletRequest request = post("/auth/link");
        request.setCookies(csrfCookie("csrf-123"));
        request.addHeader("X-XSRF-TOKEN", "csrf-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "POST /auth/link deve prosseguir quando cookie e header coincidem");
    }

    @Test
    void shouldRejectLinkEndpointWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = post("/auth/link");
        request.setCookies(csrfCookie("csrf-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertRejected(chain, response);
    }

    @Test
    void shouldRejectLinkEndpointWhenCookieMissing() throws Exception {
        MockHttpServletRequest request = post("/auth/link");
        request.addHeader("X-XSRF-TOKEN", "csrf-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertRejected(chain, response);
    }

    @Test
    void shouldRejectLinkEndpointWhenTokensDiffer() throws Exception {
        MockHttpServletRequest request = post("/auth/link");
        request.setCookies(csrfCookie("csrf-cookie"));
        request.addHeader("X-XSRF-TOKEN", "csrf-other");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertRejected(chain, response);
    }

    private void assertRejected(MockFilterChain chain, MockHttpServletResponse response) {
        assertEquals(null, chain.getRequest(), "chain NÃO deve prosseguir quando CSRF falha");
        assertEquals(403, response.getStatus());
        String body = new String(response.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("CSRF_INVALID"));
    }
}
