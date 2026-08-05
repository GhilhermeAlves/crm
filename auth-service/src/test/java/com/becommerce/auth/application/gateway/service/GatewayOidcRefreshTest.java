package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.SessionStatus;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionResolver;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.InMemoryGatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.OidcAuthorizationRequestStore;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.becommerce.auth.infrastructure.gateway.OidcProviderMetadata;
import com.becommerce.auth.infrastructure.gateway.OidcTokenValidator;
import com.becommerce.auth.infrastructure.gateway.PkceGenerator;
import com.becommerce.auth.infrastructure.gateway.RedirectUriValidator;
import com.becommerce.auth.infrastructure.gateway.SecureTokenGenerator;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayOidcRefreshTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private OidcTokenClient tokenClient;
    @Mock private OidcTokenValidator tokenValidator;
    @Mock private KeycloakIdentityConverter identityConverter;
    @Mock private CurrentUserResolutionUseCase currentUserResolutionUseCase;
    @Mock private OidcProviderMetadata providerMetadata;

    private OidcGatewayProperties properties;
    private GatewaySessionStore sessionStore;
    private GatewayOidcService service;

    @BeforeEach
    void setUp() {
        properties = new OidcGatewayProperties();
        properties.setIssuerUri("https://idp.example/realms/CRM");
        properties.setClientId("crm-gateway");
        properties.setAllowedRedirectUris(List.of("http://localhost:3000"));
        properties.setDefaultRedirect("/");
        properties.setSessionTtl(Duration.ofHours(8));
        properties.setSessionIdleTimeout(Duration.ofMinutes(30));

        sessionStore = new InMemoryGatewaySessionStore(properties);
        service = new GatewayOidcService(properties,
                new SecureTokenGenerator(),
                new PkceGenerator(new SecureTokenGenerator()),
                new RedirectUriValidator(properties),
                new OidcAuthorizationRequestStore(),
                tokenClient,
                tokenValidator,
                identityConverter,
                currentUserResolutionUseCase,
                sessionStore,
                new GatewaySessionResolver(sessionStore),
                providerMetadata,
                new ConfiguredIdentityProviderCatalog(properties));
    }

    private GatewaySession session(String token, Instant expiresAt, Instant lastAccessedAt, Instant revokedAt) {
        Instant now = Instant.now();
        return new GatewaySession(token, USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, expiresAt, lastAccessedAt, "hint-token", "access-1", "refresh-1",
                now.plusSeconds(300), "csrf", revokedAt);
    }

    private GatewaySession activeSession(String token) {
        Instant now = Instant.now();
        return session(token, now.plusSeconds(3600), now, null);
    }

    private void stubRefreshSuccess() {
        when(tokenClient.refresh(any(OidcTokenClient.RefreshRequest.class)))
                .thenReturn(new OidcTokenClient.TokenResponse("access-2", "refresh-2", "hint-2", 600));
    }

    @Test
    void shouldRotateTokensServerSideAndKeepSessionActive() {
        sessionStore.put(activeSession("t1"));
        stubRefreshSuccess();

        GatewayOidcUseCase.RefreshResult result = service.refresh("t1");

        assertEquals("access-2", result.session().accessToken());
        assertEquals("refresh-2", result.session().refreshToken());
        assertEquals("hint-2", result.session().idTokenHint());
        assertEquals(SessionStatus.ACTIVE, sessionStore.findByToken("t1").status());
    }

    @Test
    void shouldSendStoredRefreshTokenToTokenEndpoint() {
        sessionStore.put(activeSession("t1"));
        stubRefreshSuccess();

        service.refresh("t1");

        ArgumentCaptor<OidcTokenClient.RefreshRequest> captor =
                ArgumentCaptor.forClass(OidcTokenClient.RefreshRequest.class);
        verify(tokenClient).refresh(captor.capture());
        assertEquals("refresh-1", captor.getValue().refreshToken());
    }

    @Test
    void shouldUpdateLastAccessedAtOnRefresh() {
        sessionStore.put(activeSession("t1"));
        stubRefreshSuccess();
        Instant before = sessionStore.findByToken("t1").session().lastAccessedAt();

        GatewayOidcUseCase.RefreshResult result = service.refresh("t1");

        assertTrue(result.session().lastAccessedAt().isAfter(before));
    }

    @Test
    void shouldNotExtendAbsoluteTtlOnRefresh() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);
        sessionStore.put(session("t1", expiresAt, now, null));
        stubRefreshSuccess();

        GatewayOidcUseCase.RefreshResult result = service.refresh("t1");

        assertEquals(expiresAt, result.session().expiresAt(),
                "refresh nunca deve estender o TTL absoluto da sessão");
    }

    @Test
    void shouldThrowSessionNotFound() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("ghost"));
        assertEquals("SESSION_NOT_FOUND", ex.getCode());
        assertEquals(401, ex.getStatus());
    }

    @Test
    void shouldThrowSessionExpiredByAbsoluteTtl() {
        sessionStore.put(session("t2", Instant.now().minusSeconds(1), Instant.now(), null));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("t2"));
        assertEquals("SESSION_EXPIRED", ex.getCode());
    }

    @Test
    void shouldThrowSessionExpiredByIdleTimeout() {
        Instant now = Instant.now();
        sessionStore.put(session("t3", now.plusSeconds(3600), now.minusSeconds(1801), null));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("t3"));
        assertEquals("SESSION_EXPIRED", ex.getCode());
    }

    @Test
    void shouldThrowSessionRevoked() {
        sessionStore.put(activeSession("t4"));
        sessionStore.revoke("t4");

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("t4"));
        assertEquals("SESSION_REVOKED", ex.getCode());
    }

    @Test
    void shouldThrowRefreshTokenInvalidWhenProviderRejectsAndRevokeSession() {
        sessionStore.put(activeSession("t5"));
        when(tokenClient.refresh(any(OidcTokenClient.RefreshRequest.class)))
                .thenThrow(new OidcGatewayException("REFRESH_TOKEN_INVALID", 401, "rt rejeitado"));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("t5"));

        assertEquals("REFRESH_TOKEN_INVALID", ex.getCode());
        assertEquals(SessionStatus.REVOKED, sessionStore.findByToken("t5").status(),
                "falha no refresh deve invalidar a sessão (rotação)");
    }

    @Test
    void shouldRevokeSessionWhenRefreshFails() {
        sessionStore.put(activeSession("t6"));
        when(tokenClient.refresh(any(OidcTokenClient.RefreshRequest.class)))
                .thenThrow(new OidcGatewayException("OIDC_PROVIDER_UNAVAILABLE", 502, "idp down"));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("t6"));

        assertEquals("OIDC_PROVIDER_UNAVAILABLE", ex.getCode());
        assertEquals(SessionStatus.REVOKED, sessionStore.findByToken("t6").status());
    }

    @Test
    void shouldThrowRefreshTokenInvalidWhenSessionHasNoRefreshToken() {
        Instant now = Instant.now();
        GatewaySession noRefresh = new GatewaySession("t7", USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.plusSeconds(3600), now, "hint-token", "access-1", "",
                now.plusSeconds(300), "csrf", null);
        sessionStore.put(noRefresh);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> service.refresh("t7"));

        assertEquals("REFRESH_TOKEN_INVALID", ex.getCode());
        assertEquals(SessionStatus.REVOKED, sessionStore.findByToken("t7").status());
    }

    @Test
    void shouldSerializeConcurrentRefreshesPerSession() throws Exception {
        sessionStore.put(activeSession("tc"));
        stubRefreshSuccess();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                service.refresh("tc");
                return null;
            }));
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        for (Future<?> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }

        assertEquals(SessionStatus.ACTIVE, sessionStore.findByToken("tc").status());
        assertEquals("access-2", sessionStore.findByToken("tc").session().accessToken());
    }

    @Test
    void shouldSerializeConcurrentRefreshesWithoutReusingRefreshToken() throws Exception {
        sessionStore.put(activeSession("tx"));
        List<String> presented = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicInteger exchange = new AtomicInteger();
        when(tokenClient.refresh(any(OidcTokenClient.RefreshRequest.class)))
                .thenAnswer(invocation -> {
                    OidcTokenClient.RefreshRequest request = invocation.getArgument(0);
                    presented.add(request.refreshToken());
                    int n = exchange.incrementAndGet();
                    return new OidcTokenClient.TokenResponse(
                            "rotated-access-" + n, "rotated-refresh-" + n, "rotated-hint-" + n, 600);
                });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<GatewayOidcUseCase.RefreshResult>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return service.refresh("tx");
            }));
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        GatewaySession stored = sessionStore.findByToken("tx").session();
        assertEquals(SessionStatus.ACTIVE, sessionStore.findByToken("tx").status());

        assertEquals(presented.size(), new java.util.HashSet<>(presented).size(),
                "nenhum refresh token pode ser apresentado ao IdP mais de uma vez (anti-replay)");
        assertEquals("refresh-1", presented.get(0),
                "o token original deve ser o primeiro (e único) uso do token antigo");
        assertTrue(presented.size() == 2, "os 2 refreshes concorrentes devem trocar tokens em sequência");

        for (Future<GatewayOidcUseCase.RefreshResult> future : futures) {
            GatewaySession result = future.get(1, TimeUnit.SECONDS).session();
            assertNotEquals("access-1", result.accessToken(),
                    "nenhuma chamada pode devolver o access token antigo (a perdedora vê o rotacionado)");
        }
        assertEquals("rotated-refresh-2", stored.refreshToken(),
                "a sessão final deve armazenar o último refresh token rotacionado");
    }

    @Test
    void shouldRefreshTwoDifferentSessionsConcurrentlyWithoutCrossTalk() throws Exception {
        Instant now = Instant.now();
        GatewaySession first = new GatewaySession("s1", USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.plusSeconds(3600), now, "hint-a", "access-A", "refresh-A",
                now.plusSeconds(300), "csrf", null);
        GatewaySession second = new GatewaySession("s2", USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.plusSeconds(3600), now, "hint-b", "access-B", "refresh-B",
                now.plusSeconds(300), "csrf", null);
        sessionStore.put(first);
        sessionStore.put(second);

        List<String> presented = java.util.Collections.synchronizedList(new ArrayList<>());
        when(tokenClient.refresh(any(OidcTokenClient.RefreshRequest.class)))
                .thenAnswer(invocation -> {
                    OidcTokenClient.RefreshRequest request = invocation.getArgument(0);
                    presented.add(request.refreshToken());
                    return new OidcTokenClient.TokenResponse(
                            "access-" + request.refreshToken(), "refresh-" + request.refreshToken() + "-new",
                            "hint", 600);
                });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<GatewayOidcUseCase.RefreshResult>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            start.await();
            return service.refresh("s1");
        }));
        futures.add(pool.submit(() -> {
            start.await();
            return service.refresh("s2");
        }));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        for (Future<GatewayOidcUseCase.RefreshResult> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }

        assertEquals(SessionStatus.ACTIVE, sessionStore.findByToken("s1").status());
        assertEquals(SessionStatus.ACTIVE, sessionStore.findByToken("s2").status());
        assertTrue(presented.containsAll(List.of("refresh-A", "refresh-B")),
                "cada sessão deve usar exclusivamente o seu próprio refresh token");
        assertEquals("access-refresh-A", sessionStore.findByToken("s1").session().accessToken());
        assertEquals("access-refresh-B", sessionStore.findByToken("s2").session().accessToken());
    }
}
