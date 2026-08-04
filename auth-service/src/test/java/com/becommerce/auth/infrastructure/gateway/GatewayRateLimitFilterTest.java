package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import com.becommerce.auth.infrastructure.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GatewayRateLimitFilterTest {

    @Mock private GatewayRateLimiter rateLimiter;
    @Mock private GatewayCookieFactory cookieFactory;

    private final OidcGatewayProperties properties = new OidcGatewayProperties();
    private GatewayRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties.setCookieName("crm_session");
        properties.setRateLimitEnabled(true);
        filter = new GatewayRateLimitFilter(rateLimiter, cookieFactory, properties, new ObjectMapper());
    }

    private MockHttpServletRequest get(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void shouldAllowAuthorizeWithinLimit() throws Exception {
        MockHttpServletRequest request = get("/auth/authorize");
        request.setRemoteAddr("10.0.0.1");

        MockHttpServletResponse response = run(request);

        assertEquals(200, response.getStatus(), "dentro do limite o chain deve prosseguir");
        verify(rateLimiter).enforce(eq("authorize"), eq("10.0.0.1"), eq(properties.getRateLimitAuthorize()), any(Duration.class));
    }

    @Test
    void shouldUseClientIpFromTrustedForwardedFor() throws Exception {
        MockHttpServletRequest request = get("/auth/callback");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        run(request);

        verify(rateLimiter).enforce(eq("callback"), eq("203.0.113.9"), eq(properties.getRateLimitCallback()), any(Duration.class));
    }

    @Test
    void shouldUseSessionTokenForRefreshWhenPresent() throws Exception {
        org.mockito.Mockito.when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("opaque-session"));
        MockHttpServletRequest request = get("/auth/refresh");
        request.setCookies(new Cookie("crm_session", "opaque-session"));

        run(request);

        verify(rateLimiter).enforce(eq("refresh"), eq("opaque-session"), eq(properties.getRateLimitRefresh()), any(Duration.class));
    }

    @Test
    void shouldFallbackToIpForRefreshWithoutSession() throws Exception {
        org.mockito.Mockito.when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.empty());
        MockHttpServletRequest request = get("/auth/refresh");
        request.setRemoteAddr("10.0.0.7");

        run(request);

        verify(rateLimiter).enforce(eq("refresh"), eq("10.0.0.7"), eq(properties.getRateLimitRefresh()), any(Duration.class));
    }

    @Test
    void shouldReturn429WithRetryAfterAndCorrelationIdWhenLimitExceeded() throws Exception {
        CorrelationIdContext.set("corr-12345678");
        try {
            doThrow(new RateLimitExceededException(42))
                    .when(rateLimiter).enforce(any(), any(), anyInt(), any(Duration.class));
            MockHttpServletRequest request = get("/auth/authorize");
            request.setRemoteAddr("10.0.0.1");

            MockHttpServletResponse response = run(request);

            assertEquals(429, response.getStatus());
            assertEquals("42", response.getHeader("Retry-After"));
            assertEquals("corr-12345678", response.getHeader(CorrelationIdFilter.HEADER));
            String body = new String(response.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(body.contains("RATE_LIMIT_EXCEEDED"));
            assertTrue(body.contains("corr-12345678"));
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void shouldNotInterceptNonGatewayPaths() throws Exception {
        MockHttpServletRequest request = get("/api/v1/users");

        MockHttpServletResponse response = run(request);

        assertEquals(200, response.getStatus());
        verify(rateLimiter, never()).enforce(any(), any(), anyInt(), any());
    }

    @Test
    void shouldPassThroughWhenRateLimitDisabled() throws Exception {
        properties.setRateLimitEnabled(false);
        MockHttpServletRequest request = get("/auth/authorize");

        MockHttpServletResponse response = run(request);

        assertEquals(200, response.getStatus());
        verify(rateLimiter, never()).enforce(any(), any(), anyInt(), any());
    }

    @Test
    void shouldUseDifferentKeysForDifferentSessions() throws Exception {
        org.mockito.Mockito.when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("session-A"));
        MockHttpServletRequest requestA = get("/auth/logout");
        requestA.setCookies(new Cookie("crm_session", "session-A"));
        run(requestA);
        verify(rateLimiter).enforce(eq("logout"), eq("session-A"), eq(properties.getRateLimitLogout()), any(Duration.class));

        org.mockito.Mockito.when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("session-B"));
        MockHttpServletRequest requestB = get("/auth/logout");
        requestB.setCookies(new Cookie("crm_session", "session-B"));
        run(requestB);
        verify(rateLimiter).enforce(eq("logout"), eq("session-B"), eq(properties.getRateLimitLogout()), any(Duration.class));
    }
}
