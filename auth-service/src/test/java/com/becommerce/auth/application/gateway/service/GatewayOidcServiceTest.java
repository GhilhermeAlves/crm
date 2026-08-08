package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcAuthorizationRequest;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayOidcServiceTest {

    private static final String ISSUER = "https://srv1348261.hstgr.cloud/realms/CRM";
    private static final String AUTH_ENDPOINT = "https://srv1348261.hstgr.cloud/realms/CRM/protocol/openid-connect/auth";
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private OidcTokenClient tokenClient;
    @Mock private OidcTokenValidator tokenValidator;
    @Mock private KeycloakIdentityConverter identityConverter;
    @Mock private CurrentUserResolutionUseCase currentUserResolutionUseCase;
    @Mock private BackendIdentityClient backendIdentityClient;

    private OidcGatewayProperties properties;
    private OidcAuthorizationRequestStore requestStore;
    private GatewaySessionStore sessionStore;
    private PendingLinkStore pendingLinkStore;
    private GatewayOidcService service;

    @BeforeEach
    void setUp() {
        properties = new OidcGatewayProperties();
        properties.setIssuerUri(ISSUER);
        properties.setAuthorizationEndpoint(AUTH_ENDPOINT);
        properties.setTokenEndpoint(ISSUER + "/protocol/openid-connect/token");
        properties.setJwksUri(ISSUER + "/protocol/openid-connect/certs");
        properties.setClientId("crm-gateway");
        properties.setRedirectUri("http://localhost:8082/auth/callback");
        properties.setAllowedRedirectUris(List.of("http://localhost:3000"));
        properties.setSessionTtl(java.time.Duration.ofHours(8));
        properties.setAuthorizationRequestTtl(java.time.Duration.ofMinutes(10));
        properties.setAppBaseUrl("http://localhost:3000");

        requestStore = new OidcAuthorizationRequestStore();
        sessionStore = new InMemoryGatewaySessionStore(properties);
        pendingLinkStore = new InMemoryPendingLinkStore();

        service = new GatewayOidcService(properties,
                new SecureTokenGenerator(),
                new PkceGenerator(new SecureTokenGenerator()),
                new RedirectUriValidator(properties),
                requestStore,
                tokenClient,
                tokenValidator,
                identityConverter,
                currentUserResolutionUseCase,
                sessionStore,
                new GatewaySessionResolver(sessionStore),
                new OidcProviderMetadata(properties),
                new ConfiguredIdentityProviderCatalog(properties),
                backendIdentityClient,
                pendingLinkStore);
    }

    private AuthenticatedIdentity identity() {
        return new AuthenticatedIdentity(SUB, EMAIL, EMAIL, "Ghilherme", "Santos", "Ghilherme Santos", "sid-1", "google");
    }

    private CurrentUser resolvedCurrentUser() {
        return new CurrentUser(USER_ID, EMAIL, COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of("contact:read"), SUB, "sid-1", "keycloak", "Ghilherme Santos");
    }

    // --------------------------------------------------------------- authorize

    @Test
    void shouldBuildAuthorizationUriWithStateNonceAndS256Challenge() {
        GatewayOidcUseCase.BeginAuthorization result = service.beginAuthorization("/dashboard");

        URI uri = URI.create(result.authorizationUri());
        assertEquals(AUTH_ENDPOINT, uri.getScheme() + "://" + uri.getAuthority() + uri.getPath());
        assertEquals("code", query(uri, "response_type"));
        assertEquals("crm-gateway", query(uri, "client_id"));
        assertEquals("http://localhost:8082/auth/callback", query(uri, "redirect_uri"));
        assertEquals("S256", query(uri, "code_challenge_method"));
        assertNotNull(query(uri, "state"));
        assertNotNull(query(uri, "nonce"));
        assertNotNull(query(uri, "code_challenge"));
        assertNotNull(query(uri, "scope"));
        assertTrue(query(uri, "scope").contains("openid"));
        assertEquals("/dashboard", result.redirectTarget());
    }

    @Test
    void shouldGenerateUniqueStateAndNoncePerRequest() {
        URI first = URI.create(service.beginAuthorization("/").authorizationUri());
        URI second = URI.create(service.beginAuthorization("/").authorizationUri());
        assertNotEquals(query(first, "state"), query(second, "state"));
        assertNotEquals(query(first, "nonce"), query(second, "nonce"));
    }

    @Test
    void shouldRejectOpenRedirectOnAuthorize() {
        assertThrows(OidcGatewayException.class, () -> service.beginAuthorization("//evil.example"));
    }

    // ------------------------------------------- authorize + identity provider

    @Test
    void shouldNotIncludeIdpHintWhenNoProviderIsRequested() {
        URI uri = URI.create(service.beginAuthorization("/dashboard").authorizationUri());
        assertEquals(null, query(uri, "kc_idp_hint"));
    }

    @Test
    void shouldIncludeKcIdpHintWhenProviderIsEnabled() {
        properties.setEnabledProviders(new java.util.HashSet<>(java.util.List.of("google")));

        URI uri = URI.create(service.beginAuthorization("/dashboard", "google").authorizationUri());
        assertEquals("google", query(uri, "kc_idp_hint"));
        assertEquals("code", query(uri, "response_type"));
        assertEquals("/dashboard", service.beginAuthorization("/dashboard", "google").redirectTarget());
    }

    @Test
    void shouldRejectUnknownProviderWith400() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.beginAuthorization("/dashboard", "facebook"));
        assertEquals("UNKNOWN_PROVIDER", ex.getCode());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void shouldRejectDisabledProviderWith400() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.beginAuthorization("/dashboard", "phone"));
        assertEquals("PROVIDER_NOT_AVAILABLE", ex.getCode());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void shouldRejectGoogleWith400WhenGoogleIsNotEnabled() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.beginAuthorization("/dashboard", "google"));
        assertEquals("PROVIDER_NOT_AVAILABLE", ex.getCode());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void shouldIncludeKcIdpHintWhenPhoneIsEnabled() {
        properties.setEnabledProviders(new java.util.HashSet<>(java.util.List.of("phone")));

        URI uri = URI.create(service.beginAuthorization("/dashboard", "phone").authorizationUri());
        assertEquals("phone", query(uri, "kc_idp_hint"));
    }

    @Test
    void shouldTreatBlankProviderAsNoProvider() {
        URI uri = URI.create(service.beginAuthorization("/dashboard", "  ").authorizationUri());
        assertEquals(null, query(uri, "kc_idp_hint"));
    }

    // --------------------------------------------------------------- callback

    private void mockHappyPathTokens() {
        Jwt idToken = Jwt.withTokenValue("idt").header("alg", "RS256")
                .issuer(ISSUER).subject(SUB).build();
        when(tokenClient.exchange(any(OidcTokenClient.ExchangeRequest.class)))
                .thenReturn(new OidcTokenClient.TokenResponse("at", "rt", "idt", 300));
        when(tokenValidator.validateIdToken(any(), any())).thenReturn(idToken);
        when(tokenValidator.validateAccessToken("at")).thenReturn(idToken);
        when(identityConverter.convert(idToken))
                .thenReturn(new UsernamePasswordAuthenticationToken(identity(), idToken, List.of()));
    }

    @Test
    void shouldCompleteAuthorizationAndCreateOpaqueSession() {
        mockHappyPathTokens();
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.Resolved(resolvedCurrentUser()));

        GatewayOidcUseCase.BeginAuthorization begin = service.beginAuthorization("/dashboard");
        GatewayOidcUseCase.AuthenticationResult result =
                service.completeAuthorization("code-1", query(URI.create(begin.authorizationUri()), "state"));

        assertEquals("/dashboard", result.redirectTarget());
        GatewaySession session = result.session();
        assertEquals(USER_ID, session.userId());
        assertEquals(COMPANY_ID, session.companyId());
        assertEquals(List.of("AGENT"), session.roles());
        assertTrue(!session.sessionToken().contains("."), "session token must be opaque, not a JWT");
        assertTrue(sessionStore.get(session.sessionToken()).isPresent());
        assertTrue(requestStore.size() == 0, "state must be consumed (single-use)");
    }

    @Test
    void shouldRejectCallbackWithoutCodeOrState() {
        assertThrows(OidcGatewayException.class, () -> service.completeAuthorization(null, "s"));
        assertThrows(OidcGatewayException.class, () -> service.completeAuthorization("c", "  "));
    }

    @Test
    void shouldRejectUnknownOrReusedState() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeAuthorization("code-1", "unknown-state"));
        assertEquals("INVALID_STATE", ex.getCode());
        assertEquals(400, ex.getStatus());

        when(tokenClient.exchange(any(OidcTokenClient.ExchangeRequest.class)))
                .thenThrow(new OidcGatewayException("TOKEN_EXCHANGE_FAILED", 502, "falha"));
        String state = query(URI.create(service.beginAuthorization("/").authorizationUri()), "state");

        OidcGatewayException first = assertThrows(OidcGatewayException.class,
                () -> service.completeAuthorization("code-1", state));
        assertEquals("TOKEN_EXCHANGE_FAILED", first.getCode());

        OidcGatewayException replay = assertThrows(OidcGatewayException.class,
                () -> service.completeAuthorization("code-2", state));
        assertEquals("INVALID_STATE", replay.getCode());
    }

    @Test
    void shouldRejectProvisioningRequiredWithoutSession() {
        // Setup mocks for local login (no identity_provider claim)
        String state = "test-state-" + UUID.randomUUID();
        String nonce = "test-nonce-" + UUID.randomUUID();
        String codeVerifier = "test-verifier-" + UUID.randomUUID();
        String codeChallenge = "test-challenge";
        OidcAuthorizationRequest request = new OidcAuthorizationRequest(
                state, nonce, codeVerifier, "/",
                Instant.now().plus(properties.getAuthorizationRequestTtl()));
        requestStore.put(request);

        Jwt idToken = Jwt.withTokenValue("idt").header("alg", "RS256")
                .issuer(ISSUER).subject(SUB)
                .claim("email", EMAIL)
                .claim("preferred_username", EMAIL)
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .claim("name", "Ghilherme Santos")
                .claim("sid", "sid-1")
                .build();

        when(tokenClient.exchange(any(OidcTokenClient.ExchangeRequest.class)))
                .thenReturn(new OidcTokenClient.TokenResponse("at", "rt", "idt", 300));
        when(tokenValidator.validateIdToken(any(), any())).thenReturn(idToken);
        when(tokenValidator.validateAccessToken("at")).thenReturn(idToken);
        when(identityConverter.convert(idToken))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        new AuthenticatedIdentity(SUB, EMAIL, EMAIL, "Ghilherme", "Santos", "Ghilherme Santos", "sid-1", "keycloak"),
                        idToken, List.of()));

        AuthenticatedIdentity localIdentity = new AuthenticatedIdentity(SUB, EMAIL, EMAIL, "Ghilherme", "Santos", "Ghilherme Santos", "sid-1", "keycloak");
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.ProvisioningRequired(localIdentity));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeAuthorization("code-1", state));
        assertEquals("PROVISIONING_REQUIRED", ex.getCode());
        assertEquals(403, ex.getStatus());
        assertEquals(0, sessionStore.size());
    }

    @Test
    void shouldPropagateCrmAccessDeniedWithoutSession() {
        mockHappyPathTokens();
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenThrow(new CrmAccessDeniedException("Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente."));

        String state = query(URI.create(service.beginAuthorization("/").authorizationUri()), "state");

        assertThrows(CrmAccessDeniedException.class,
                () -> service.completeAuthorization("code-1", state));
        assertEquals(0, sessionStore.size(), "CRM access negado deve impedir criação de sessão");
    }

    @Test
    void shouldPropagateTokenValidationFailure() {
        when(tokenClient.exchange(any(OidcTokenClient.ExchangeRequest.class)))
                .thenReturn(new OidcTokenClient.TokenResponse("at", null, "idt", 300));
        when(tokenValidator.validateIdToken(any(), any()))
                .thenThrow(new OidcGatewayException("TOKEN_VALIDATION_FAILED", 401, "Token rejeitado"));

        String state = query(URI.create(service.beginAuthorization("/").authorizationUri()), "state");

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeAuthorization("code-1", state));
        assertEquals("TOKEN_VALIDATION_FAILED", ex.getCode());
        assertEquals(0, sessionStore.size());
    }

    @Test
    void shouldPassCodeVerifierFromStateIntoExchange() {
        String state = query(URI.create(service.beginAuthorization("/").authorizationUri()), "state");
        OidcAuthorizationRequestStore spyStore = mock(OidcAuthorizationRequestStore.class);
        // covered indirectly; assert exchange receives a request for the stored verifier
        when(spyStore.consume(state)).thenAnswer(invocation -> {
            var req = new com.becommerce.auth.domain.gateway.OidcAuthorizationRequest(
                    state, "nonce", "stored-verifier", "/", java.time.Instant.now().plusSeconds(600));
            req.consume();
            return req;
        });
        when(tokenClient.exchange(any(OidcTokenClient.ExchangeRequest.class)))
                .thenAnswer(invocation -> {
                    OidcTokenClient.ExchangeRequest req = invocation.getArgument(0);
                    assertEquals("stored-verifier", req.codeVerifier());
                    return new OidcTokenClient.TokenResponse("at", null, "idt", 300);
                });
        when(tokenValidator.validateIdToken(any(), any()))
                .thenThrow(new OidcGatewayException("TOKEN_VALIDATION_FAILED", 401, "x"));

        GatewayOidcService serviceWithSpy = new GatewayOidcService(properties,
                new SecureTokenGenerator(),
                new PkceGenerator(new SecureTokenGenerator()),
                new RedirectUriValidator(properties),
                spyStore,
                tokenClient,
                tokenValidator,
                identityConverter,
                currentUserResolutionUseCase,
                sessionStore,
                new GatewaySessionResolver(sessionStore),
                new OidcProviderMetadata(properties),
                new ConfiguredIdentityProviderCatalog(properties),
                backendIdentityClient,
                pendingLinkStore);

        assertThrows(OidcGatewayException.class, () -> serviceWithSpy.completeAuthorization("code-1", state));
    }

    private String query(URI uri, String name) {
        return Optional.ofNullable(uri.getQuery())
                .flatMap(q -> java.util.Arrays.stream(q.split("&"))
                        .map(p -> p.split("=", 2))
                        .filter(p -> p[0].equals(name) && p.length == 2)
                        .findFirst())
                .map(p -> p[1])
                .orElse(null);
    }
}
