package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.SessionStatus;
import com.becommerce.auth.infrastructure.gateway.BackendIdentityClient;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionResolver;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.InMemoryGatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.InMemoryPendingLinkStore;
import com.becommerce.auth.infrastructure.gateway.OidcAuthorizationRequestStore;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.becommerce.auth.infrastructure.gateway.OidcProviderMetadata;
import com.becommerce.auth.infrastructure.gateway.OidcTokenValidator;
import com.becommerce.auth.infrastructure.gateway.PendingLinkStore;
import com.becommerce.auth.infrastructure.gateway.PkceGenerator;
import com.becommerce.auth.infrastructure.gateway.RedirectUriValidator;
import com.becommerce.auth.infrastructure.gateway.SecureTokenGenerator;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayOidcLogoutTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private OidcTokenClient tokenClient;
    @Mock private OidcTokenValidator tokenValidator;
    @Mock private KeycloakIdentityConverter identityConverter;
    @Mock private CurrentUserResolutionUseCase currentUserResolutionUseCase;
    @Mock private OidcProviderMetadata providerMetadata;
    @Mock private BackendIdentityClient backendIdentityClient;

    private OidcGatewayProperties properties;
    private GatewaySessionStore sessionStore;
    private PendingLinkStore pendingLinkStore;
    private GatewayOidcService service;

    @BeforeEach
    void setUp() {
        properties = new OidcGatewayProperties();
        properties.setIssuerUri("https://idp.example/realms/CRM");
        properties.setClientId("crm-gateway");
        properties.setAllowedRedirectUris(List.of("http://localhost:3000", "https://app.example"));
        properties.setDefaultRedirect("/");
        properties.setSessionTtl(java.time.Duration.ofHours(8));

        sessionStore = new InMemoryGatewaySessionStore(properties);
        pendingLinkStore = new InMemoryPendingLinkStore();
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
                new ConfiguredIdentityProviderCatalog(properties),
                backendIdentityClient,
                pendingLinkStore);
    }

    private GatewaySession session(String token) {
        Instant now = Instant.now();
        return new GatewaySession(token, USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.plusSeconds(3600), now, "hint-token", "access", "refresh",
                now.plusSeconds(300), "csrf", null);
    }

    private String query(String uri, String name) {
        return Optional.ofNullable(URI.create(uri).getQuery())
                .flatMap(q -> Arrays.stream(q.split("&"))
                        .map(p -> p.split("=", 2))
                        .filter(p -> p[0].equals(name) && p.length == 2)
                        .findFirst())
                .map(p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8))
                .orElse(null);
    }

    @Test
    void shouldInvalidateSessionAndBuildEndSessionRedirect() {
        sessionStore.put(session("t1"));
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t1", null);

        assertTrue(result.redirectUri().startsWith("https://idp.example/logout"));
        assertEquals("crm-gateway", query(result.redirectUri(), "client_id"));
        assertEquals("/", query(result.redirectUri(), "post_logout_redirect_uri"));
        assertEquals("hint-token", query(result.redirectUri(), "id_token_hint"));
        assertEquals(SessionStatus.REVOKED, sessionStore.findByToken("t1").status(),
                "logout deve invalidar a sessão local (tombstone)");
    }

    @Test
    void shouldRedirectToDefaultTargetWhenPostLogoutNotProvided() {
        sessionStore.put(session("t2"));
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t2", null);

        assertEquals("/", query(result.redirectUri(), "post_logout_redirect_uri"));
    }

    @Test
    void shouldRedirectLocallyWhenProviderUnavailable() {
        sessionStore.put(session("t3"));
        when(providerMetadata.endSessionEndpoint())
                .thenThrow(new OidcGatewayException("OIDC_PROVIDER_UNAVAILABLE", 502, "idp down"));

        GatewayOidcUseCase.LogoutResult result = service.logout("t3", null);

        assertEquals("/", result.redirectUri(), "sem provedor, redireciona localmente");
        assertEquals(SessionStatus.REVOKED, sessionStore.findByToken("t3").status(),
                "sessão local deve ser invalidada mesmo com provedor indisponível");
    }

    @Test
    void shouldBeIdempotentWhenSessionUnknown() {
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout(null, null);

        assertTrue(result.redirectUri().startsWith("https://idp.example/logout"));
        assertNull(query(result.redirectUri(), "id_token_hint"),
                "sem sessão não há id_token_hint");
    }

    @Test
    void shouldBeIdempotentWhenSessionAlreadyRevoked() {
        sessionStore.put(session("t4"));
        sessionStore.revoke("t4");
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t4", null);

        assertTrue(result.redirectUri().startsWith("https://idp.example/logout"));
        assertEquals(SessionStatus.REVOKED, sessionStore.findByToken("t4").status());
    }

    @Test
    void shouldRejectOpenRedirectOnLogout() {
        sessionStore.put(session("t5"));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.logout("t5", "//evil.example"));

        assertEquals("OPEN_REDIRECT", ex.getCode());
        assertEquals(SessionStatus.ACTIVE, sessionStore.findByToken("t5").status(),
                "redirect inválido não deve invalidar a sessão");
    }

    @Test
    void shouldValidateAbsolutePostLogoutAgainstAllowlist() {
        sessionStore.put(session("t6"));
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t6", "https://app.example/dashboard");

        assertEquals("https://app.example/dashboard", query(result.redirectUri(), "post_logout_redirect_uri"));
    }

    @Test
    void shouldRejectAbsolutePostLogoutNotInAllowlist() {
        sessionStore.put(session("t7"));

        assertThrows(OidcGatewayException.class, () -> service.logout("t7", "https://evil.example"));
    }

    @Test
    void shouldAbsolutizeRelativeTargetWithAppBaseUrl() {
        properties.setAppBaseUrl("http://localhost:3000");
        sessionStore.put(session("t8"));
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t8", "/dashboard");

        assertEquals("http://localhost:3000/dashboard", query(result.redirectUri(), "post_logout_redirect_uri"));
    }

    @Test
    void shouldAbsolutizeRelativeTargetUsingDynamicOriginWhenEnabled() {
        properties.setDynamicRedirectUri(true);
        sessionStore.put(session("t9"));
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t9", "/dashboard", "http://localhost:3000");

        assertEquals("http://localhost:3000/dashboard", query(result.redirectUri(), "post_logout_redirect_uri"));
    }

    @Test
    void shouldIgnoreDynamicOriginWhenNotAllowlistedOnLogout() {
        properties.setDynamicRedirectUri(true);
        properties.setAppBaseUrl("https://fixed.example");
        sessionStore.put(session("t10"));
        when(providerMetadata.endSessionEndpoint()).thenReturn("https://idp.example/logout");

        GatewayOidcUseCase.LogoutResult result = service.logout("t10", "/dashboard", "https://evil.example");

        assertEquals("https://fixed.example/dashboard", query(result.redirectUri(), "post_logout_redirect_uri"));
    }
}
