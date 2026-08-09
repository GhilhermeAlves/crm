package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.PendingLink;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Sprint 7.2, Caso B — vínculo de identidade externa à conta local (senha).
 * Cobre o caminho do callback que gera o {@code PendingLink}, a consulta de
 * estado ({@code linkStatus}) e a conclusão ({@code completeLink}) com os
 * desfechos de sucesso, senha incorreta, conta removida e uso único.
 */
@ExtendWith(MockitoExtension.class)
class GatewayOidcLinkingTest {

    private static final String ISSUER = "https://srv1348261.hstgr.cloud/realms/CRM";
    private static final String AUTH_ENDPOINT = "https://srv1348261.hstgr.cloud/realms/CRM/protocol/openid-connect/auth";
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";
    private static final String PASSWORD = "senha-certa";

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

    private AuthenticatedIdentity googleIdentity() {
        return new AuthenticatedIdentity(SUB, EMAIL, EMAIL, "Ghilherme", "Santos",
                "Ghilherme Santos", "sid-1", "google");
    }

    private CurrentUser resolvedCurrentUser() {
        return new CurrentUser(USER_ID, EMAIL, COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of("contact:read"), SUB, "sid-1", "keycloak", "Ghilherme Santos");
    }

    private void mockTokens() {
        Jwt idToken = Jwt.withTokenValue("idt").header("alg", "RS256")
                .issuer(ISSUER).subject(SUB).build();
        when(tokenClient.exchange(any(OidcTokenClient.ExchangeRequest.class)))
                .thenReturn(new OidcTokenClient.TokenResponse("at", "rt", "idt", 300));
        when(tokenValidator.validateIdToken(any(), any())).thenReturn(idToken);
        when(tokenValidator.validateAccessToken("at")).thenReturn(idToken);
        when(identityConverter.convert(idToken))
                .thenReturn(new UsernamePasswordAuthenticationToken(googleIdentity(), idToken, List.of()));
    }

    private String beginLinkingRequiredFlow() {
        mockTokens();
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.LinkingRequired(googleIdentity()));

        String state = query(URI.create(service.beginAuthorization("/").authorizationUri()), "state");
        GatewayOidcUseCase.AuthenticationResult result = service.completeAuthorization("code-1", state);

        assertNull(result.session(), "sem sessão criada antes do vínculo");
        assertNotNull(result.pendingLink(), "fluxo deve gerar vínculo pendente");
        assertEquals(0, sessionStore.size());
        return result.pendingLink().token();
    }

    // --------------------------------------------------------- link-status

    @Test
    void shouldReturnNotPendingWhenNoToken() {
        GatewayOidcUseCase.LinkStatusResult result = service.linkStatus(null);
        assertFalse(result.pending());
        assertNull(result.email());
    }

    @Test
    void shouldReturnNotPendingForUnknownToken() {
        GatewayOidcUseCase.LinkStatusResult result = service.linkStatus("token-que-nao-existe");
        assertFalse(result.pending());
        assertNull(result.email());
    }

    @Test
    void shouldReturnPendingWithEmailForActivePendingLink() {
        String token = beginLinkingRequiredFlow();

        GatewayOidcUseCase.LinkStatusResult result = service.linkStatus(token);
        assertTrue(result.pending());
        assertEquals(EMAIL, result.email());
    }

    @Test
    void shouldReturnNotPendingWhenPendingLinkExpired() {
        String token = beginLinkingRequiredFlow();
        // Expira o vínculo por dentro da store (mesmo comportamento que o Redis):
        // um vínculo além do expiresAt resolve ausente na leitura.
        Instant now = Instant.now();
        pendingLinkStore.put(new PendingLink(
                "expired-token", SUB, EMAIL, "Ghilherme", "google",
                "csrf-1", "id", "at", "rt", now.plusSeconds(300),
                "/", now.minusSeconds(60), now.minusSeconds(1)));

        GatewayOidcUseCase.LinkStatusResult result = service.linkStatus("expired-token");
        assertFalse(result.pending());
        assertNull(result.email());
        assertTrue(pendingLinkStore.get("expired-token").isEmpty(), "expirado deve ser removido");
    }

    // --------------------------------------------------------- complete-link

    @Test
    void shouldRejectMissingPassword() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeLink("token", "   "));
        assertEquals("INVALID_LINK_REQUEST", ex.getCode());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void shouldRejectUnknownPendingToken() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeLink("token-que-nao-existe", PASSWORD));
        assertEquals("LINK_PENDING_NOT_FOUND", ex.getCode());
        assertEquals(410, ex.getStatus());
    }

    @Test
    void shouldRejectInvalidCredentialsWithoutRemovingPendingLink() {
        String token = beginLinkingRequiredFlow();
        when(backendIdentityClient.link(any(), eq(PASSWORD)))
                .thenReturn(BackendIdentityClient.LinkOutcome.INVALID_CREDENTIALS);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeLink(token, PASSWORD));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        assertEquals(401, ex.getStatus());
        assertTrue(pendingLinkStore.get(token).isPresent(),
                "senha incorreta deve permitir nova tentativa (vínculo não consumido)");
        assertEquals(0, sessionStore.size());
    }

    @Test
    void shouldConsumePendingLinkWhenLocalAccountRemoved() {
        String token = beginLinkingRequiredFlow();
        when(backendIdentityClient.link(any(), eq(PASSWORD)))
                .thenReturn(BackendIdentityClient.LinkOutcome.LINK_NOT_FOUND);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeLink(token, PASSWORD));
        assertEquals("LINK_NOT_FOUND", ex.getCode());
        assertEquals(410, ex.getStatus());
        assertTrue(pendingLinkStore.get(token).isEmpty(), "conta removida consome o vínculo");
    }

    @Test
    void shouldLinkAndCreateGatewaySession() {
        String token = beginLinkingRequiredFlow();
        when(backendIdentityClient.link(any(), eq(PASSWORD)))
                .thenReturn(BackendIdentityClient.LinkOutcome.LINKED);
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.Resolved(resolvedCurrentUser()));

        GatewayOidcUseCase.LinkResult result = service.completeLink(token, PASSWORD);

        assertEquals("/", result.redirectTarget());
        assertEquals(USER_ID, result.session().userId());
        assertEquals(1, sessionStore.size());
        assertTrue(pendingLinkStore.get(token).isEmpty(),
                "vínculo consumido (uso único) após o sucesso");
    }

    @Test
    void shouldBeSingleUseAfterSuccess() {
        String token = beginLinkingRequiredFlow();
        when(backendIdentityClient.link(any(), eq(PASSWORD)))
                .thenReturn(BackendIdentityClient.LinkOutcome.LINKED);
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.Resolved(resolvedCurrentUser()));

        service.completeLink(token, PASSWORD);
        assertEquals(1, sessionStore.size());

        OidcGatewayException replay = assertThrows(OidcGatewayException.class,
                () -> service.completeLink(token, PASSWORD));
        assertEquals("LINK_PENDING_NOT_FOUND", replay.getCode());
        assertEquals(410, replay.getStatus());
        assertEquals(1, sessionStore.size(), "replay não deve criar nova sessão");
    }

    @Test
    void shouldFailWhenResolutionFailsAfterBackendLink() {
        String token = beginLinkingRequiredFlow();
        when(backendIdentityClient.link(any(), eq(PASSWORD)))
                .thenReturn(BackendIdentityClient.LinkOutcome.LINKED);
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.ProvisioningRequired(googleIdentity()));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> service.completeLink(token, PASSWORD));
        assertEquals("LINK_RESOLUTION_FAILED", ex.getCode());
        assertEquals(502, ex.getStatus());
        assertTrue(pendingLinkStore.get(token).isEmpty(), "vínculo já consumido no backend");
        assertEquals(0, sessionStore.size());
    }

    // ---------------------------------------------------------------- helpers

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
