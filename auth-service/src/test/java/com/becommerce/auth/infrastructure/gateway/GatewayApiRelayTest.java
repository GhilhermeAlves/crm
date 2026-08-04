package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Testes do BFF relay (Sprint 6.4): repasse de {@code /api/**} ao crm-backend
 * injetando o access token da sessão de gateway. Usa um backend HTTP real
 * (com.sun.net.httpserver) no papel do crm-backend.
 */
@ExtendWith(MockitoExtension.class)
class GatewayApiRelayTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private GatewayOidcUseCase gatewayOidcUseCase;

    private HttpServer server;
    private String baseUrl;
    private OidcGatewayProperties properties;
    private GatewaySessionStore store;
    private GatewayApiRelay relay;

    private final AtomicInteger serverCalls = new AtomicInteger();
    private String lastAuth;
    private String lastMethod;
    private String lastQuery;
    private String lastBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1", this::handleBackend);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();

        properties = new OidcGatewayProperties();
        properties.setApiBackendUrl(baseUrl);
        properties.setSessionTtl(Duration.ofHours(8));
        properties.setSessionIdleTimeout(Duration.ofMinutes(30));

        store = new InMemoryGatewaySessionStore(properties);
        relay = new GatewayApiRelay(properties, new GatewaySessionResolver(store), gatewayOidcUseCase);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handleBackend(HttpExchange exchange) throws IOException {
        serverCalls.incrementAndGet();
        lastMethod = exchange.getRequestMethod();
        lastAuth = exchange.getRequestHeaders().getFirst("Authorization");
        lastQuery = exchange.getRequestURI().getRawQuery();
        lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/api/v1/missing")) {
            respond(exchange, 404, "{\"status\":404,\"code\":\"NOT_FOUND\"}");
        } else if (path.equals("/api/v1/fail-401")) {
            if (serverCalls.get() == 1) {
                respond(exchange, 401, "{\"status\":401,\"code\":\"UNAUTHORIZED\"}");
            } else {
                respond(exchange, 200, "{\"ok\":true}");
            }
        } else {
            respond(exchange, 200, "{\"data\":1}");
        }
    }

    private void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private GatewaySession session(String token, Instant accessTokenExpiresAt) {
        Instant now = Instant.now();
        return new GatewaySession(token, USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.plusSeconds(3600), now, "hint", "access-1", "refresh-1",
                accessTokenExpiresAt, "csrf", null);
    }

    private void rotateSession(String token) {
        GatewaySession current = store.findByToken(token).session();
        Instant now = Instant.now();
        store.put(current.withRotatedTokens("access-2", "refresh-2", null,
                now.plusSeconds(600), now));
    }

    @Test
    void shouldForwardGetWithBearerAndReturnBackendBody() {
        store.put(session("t1", Instant.now().plusSeconds(600)));

        ResponseEntity<byte[]> response = relay.forward(HttpMethod.GET, "/api/v1/users", "page=1",
                "t1", Map.of("Accept", "application/json"), new byte[0]);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("{\"data\":1}", new String(response.getBody(), StandardCharsets.UTF_8));
        assertEquals("Bearer access-1", lastAuth);
        assertEquals("GET", lastMethod);
        assertEquals("page=1", lastQuery);
        verify(gatewayOidcUseCase, never()).refresh(anyString());
    }

    @Test
    void shouldForwardPostBodyAndContentType() {
        store.put(session("t2", Instant.now().plusSeconds(600)));

        ResponseEntity<byte[]> response = relay.forward(HttpMethod.POST, "/api/v1/users", null,
                "t2", Map.of("Content-Type", "application/json"),
                "{\"name\":\"x\"}".getBytes(StandardCharsets.UTF_8));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("POST", lastMethod);
        assertEquals("{\"name\":\"x\"}", lastBody);
        assertEquals("Bearer access-1", lastAuth);
    }

    @Test
    void shouldRejectWhenNoSessionToken() {
        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () ->
                relay.forward(HttpMethod.GET, "/api/v1/users", null, null, Map.of(), new byte[0]));

        assertEquals("SESSION_NOT_FOUND", ex.getCode());
        assertEquals(401, ex.getStatus());
        assertEquals(0, serverCalls.get());
    }

    @Test
    void shouldRejectRevokedSession() {
        store.put(session("t3", Instant.now().plusSeconds(600)));
        store.revoke("t3");

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () ->
                relay.forward(HttpMethod.GET, "/api/v1/users", null, "t3", Map.of(), new byte[0]));

        assertEquals("SESSION_REVOKED", ex.getCode());
        assertEquals(401, ex.getStatus());
        assertEquals(0, serverCalls.get());
    }

    @Test
    void shouldRejectExpiredSession() {
        Instant now = Instant.now();
        store.put(new GatewaySession("t4", USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.minusSeconds(1), now, "hint", "access-1", "refresh-1",
                now.plusSeconds(600), "csrf", null));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () ->
                relay.forward(HttpMethod.GET, "/api/v1/users", null, "t4", Map.of(), new byte[0]));

        assertEquals("SESSION_EXPIRED", ex.getCode());
        assertEquals(401, ex.getStatus());
        assertEquals(0, serverCalls.get());
    }

    @Test
    void shouldRefreshExpiredAccessTokenBeforeForwarding() {
        store.put(session("t5", Instant.now().minusSeconds(10)));
        doAnswer(invocation -> {
            rotateSession("t5");
            return null;
        }).when(gatewayOidcUseCase).refresh(anyString());

        ResponseEntity<byte[]> response = relay.forward(HttpMethod.GET, "/api/v1/users", null,
                "t5", Map.of(), new byte[0]);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Bearer access-2", lastAuth);
        verify(gatewayOidcUseCase).refresh("t5");
    }

    @Test
    void shouldForwardUpstreamErrorStatusAndBody() {
        store.put(session("t6", Instant.now().plusSeconds(600)));

        ResponseEntity<byte[]> response = relay.forward(HttpMethod.GET, "/api/v1/missing", null,
                "t6", Map.of(), new byte[0]);

        assertEquals(404, response.getStatusCode().value());
        assertTrue(new String(response.getBody(), StandardCharsets.UTF_8).contains("404"));
    }

    @Test
    void shouldRetryOnceAfterUpstream401() {
        store.put(session("t7", Instant.now().plusSeconds(600)));
        doAnswer(invocation -> {
            rotateSession("t7");
            return null;
        }).when(gatewayOidcUseCase).refresh(anyString());

        ResponseEntity<byte[]> response = relay.forward(HttpMethod.GET, "/api/v1/fail-401", null,
                "t7", Map.of(), new byte[0]);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Bearer access-2", lastAuth);
        verify(gatewayOidcUseCase).refresh("t7");
        assertTrue(serverCalls.get() >= 2, "retry único deve repetir a chamada ao backend");
    }

    @Test
    void shouldReturn502WhenBackendUnavailable() throws IOException {
        HttpServer tmp = HttpServer.create(new InetSocketAddress(0), 0);
        int closedPort = tmp.getAddress().getPort();
        tmp.stop(0);
        properties.setApiBackendUrl("http://localhost:" + closedPort);
        store.put(session("t8", Instant.now().plusSeconds(600)));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () ->
                relay.forward(HttpMethod.GET, "/api/v1/users", null, "t8", Map.of(), new byte[0]));

        assertEquals("UPSTREAM_UNAVAILABLE", ex.getCode());
        assertEquals(502, ex.getStatus());
    }

    @Test
    void shouldNotForwardRequestWithoutConfiguredBackend() {
        properties.setApiBackendUrl("");
        store.put(session("t9", Instant.now().plusSeconds(600)));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () ->
                relay.forward(HttpMethod.GET, "/api/v1/users", null, "t9", Map.of(), new byte[0]));

        assertEquals("RELAY_NOT_CONFIGURED", ex.getCode());
        assertEquals(500, ex.getStatus());
    }
}
