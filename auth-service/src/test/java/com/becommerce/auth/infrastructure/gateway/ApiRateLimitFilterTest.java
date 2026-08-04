package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.becommerce.auth.domain.gateway.SessionStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do rate limiting do relay {@code /api/**} (Sprint 6.7): buckets por
 * usuário autenticado com fallback para IP real, isolamento entre usuários,
 * 429 com Retry-After + correlation ID e comportamento fail-controlled.
 */
class ApiRateLimitFilterTest {

    @Mock private GatewayRateLimiter rateLimiter;
    @Mock private GatewayCookieFactory cookieFactory;
    @Mock private GatewaySessionResolver sessionResolver;
    @Mock private ClientIpResolver clientIpResolver;

    private final OidcGatewayProperties properties = new OidcGatewayProperties();
    private ApiRateLimitFilter filter;

    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties.setRateLimitEnabled(true);
        properties.setRateLimitApi(60);
        properties.setRateLimitWindow(Duration.ofSeconds(60));
        filter = new ApiRateLimitFilter(
                rateLimiter, cookieFactory, sessionResolver, clientIpResolver,
                new RateLimitErrorResponse(new ObjectMapper()), properties);
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

    private void authenticatedAs(UUID userId) {
        GatewaySession session = new GatewaySession(
                "token-" + userId, userId, "user@crm.local", UUID.randomUUID(), UUID.randomUUID(),
                List.of("USER"), List.of(), "sub-" + userId, null, null, null,
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600), Instant.now(),
                "id-hint", "access", "refresh", Instant.now().plusSeconds(300), "csrf", null);
        when(sessionResolver.resolve(any())).thenReturn(SessionLookup.active(session));
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("token-" + userId));
    }

    @Test
    void shouldKeyBucketByAuthenticatedUser() throws Exception {
        authenticatedAs(USER_A);
        MockHttpServletRequest request = get("/api/v1/users");
        request.setCookies(new Cookie("crm_session", "token-" + USER_A));

        run(request);

        verify(rateLimiter).enforce(eq("api"), eq(USER_A.toString()), eq(60), eq(Duration.ofSeconds(60)));
        verify(clientIpResolver, never()).resolve(any());
    }

    @Test
    void shouldUseSameBucketForSameUserAcrossPaths() throws Exception {
        authenticatedAs(USER_A);
        run(get("/api/v1/users"));
        run(get("/api/v1/contacts"));

        verify(rateLimiter, org.mockito.Mockito.times(2))
                .enforce(eq("api"), eq(USER_A.toString()), eq(60), eq(Duration.ofSeconds(60)));
    }

    @Test
    void shouldIsolateDifferentUsersInDifferentBuckets() throws Exception {
        authenticatedAs(USER_A);
        run(get("/api/v1/users"));

        authenticatedAs(USER_B);
        run(get("/api/v1/users"));

        verify(rateLimiter).enforce(eq("api"), eq(USER_A.toString()), eq(60), any());
        verify(rateLimiter).enforce(eq("api"), eq(USER_B.toString()), eq(60), any());
    }

    @Test
    void shouldFallbackToRealIpWhenNoSessionCookie() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.empty());
        when(clientIpResolver.resolve(any())).thenReturn("10.0.0.5");
        MockHttpServletRequest request = get("/api/v1/users");

        run(request);

        verify(rateLimiter).enforce(eq("api"), eq("10.0.0.5"), eq(60), any());
        verify(sessionResolver, never()).resolve(any());
    }

    @Test
    void shouldFallbackToIpWhenSessionNotActive() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("expired-token"));
        when(sessionResolver.resolve(any())).thenReturn(SessionLookup.notFound());
        when(clientIpResolver.resolve(any())).thenReturn("10.0.0.6");

        run(get("/api/v1/users"));

        verify(rateLimiter).enforce(eq("api"), eq("10.0.0.6"), eq(60), any());
    }

    @Test
    void shouldFallbackToIpWhenIdentityResolutionFails() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("token"));
        when(sessionResolver.resolve(any())).thenThrow(new OidcGatewayException("SESSION_LOOKUP_UNAVAILABLE", 503, "redis down"));
        when(clientIpResolver.resolve(any())).thenReturn("10.0.0.7");

        run(get("/api/v1/users"));

        verify(rateLimiter).enforce(eq("api"), eq("10.0.0.7"), eq(60), any());
    }

    @Test
    void shouldReturn429WithRetryAfterAndCorrelationIdWhenLimitExceeded() throws Exception {
        CorrelationIdContext.set("corr-api-12345678");
        try {
            authenticatedAs(USER_A);
            doThrow(new RateLimitExceededException(7))
                    .when(rateLimiter).enforce(eq("api"), eq(USER_A.toString()), anyInt(), any());
            MockHttpServletRequest request = get("/api/v1/users");
            request.setCookies(new Cookie("crm_session", "token-" + USER_A));

            MockHttpServletResponse response = run(request);

            assertEquals(429, response.getStatus());
            assertEquals("7", response.getHeader("Retry-After"));
            assertEquals("corr-api-12345678", response.getHeader(CorrelationIdFilter.HEADER));
            String body = new String(response.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(body.contains("RATE_LIMIT_EXCEEDED"));
            assertTrue(body.contains("corr-api-12345678"));
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void shouldNotKeyByArbitraryClientHeaders() throws Exception {
        authenticatedAs(USER_A);
        MockHttpServletRequest request = get("/api/v1/users");
        request.addHeader("X-Forwarded-For", "203.0.113.99");
        request.addHeader("X-Forwarded-User", "victim-user-id");
        request.addHeader("X-Real-IP", "203.0.113.99");

        run(request);

        verify(rateLimiter).enforce(eq("api"), eq(USER_A.toString()), anyInt(), any());
        verify(clientIpResolver, never()).resolve(any());
    }

    @Test
    void shouldPassThroughWhenRateLimitApiDisabled() throws Exception {
        properties.setRateLimitApi(0);
        authenticatedAs(USER_A);

        MockHttpServletResponse response = run(get("/api/v1/users"));

        assertEquals(200, response.getStatus());
        verify(rateLimiter, never()).enforce(any(), any(), anyInt(), any());
    }

    @Test
    void shouldPassThroughWhenRateLimitDisabled() throws Exception {
        properties.setRateLimitEnabled(false);
        authenticatedAs(USER_A);

        MockHttpServletResponse response = run(get("/api/v1/users"));

        assertEquals(200, response.getStatus());
        verify(rateLimiter, never()).enforce(any(), any(), anyInt(), any());
    }

    @Test
    void shouldNotInterceptNonApiPaths() throws Exception {
        authenticatedAs(USER_A);
        MockHttpServletRequest request = get("/auth/authorize");

        MockHttpServletResponse response = run(request);

        assertEquals(200, response.getStatus());
        verify(rateLimiter, never()).enforce(any(), any(), anyInt(), any());
    }
}
