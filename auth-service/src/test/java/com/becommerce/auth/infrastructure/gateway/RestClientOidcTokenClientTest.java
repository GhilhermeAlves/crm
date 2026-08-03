package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestClientOidcTokenClientTest {

    private HttpServer server;
    private String baseUrl;
    private String lastBody;
    private OidcGatewayProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
        properties = new OidcGatewayProperties();
        properties.setTokenEndpoint(baseUrl + "/token");
        properties.setRedirectUri("http://localhost:8082/auth/callback");
        properties.setClientId("crm-gateway");
        properties.setTokenExchangeTimeout(Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String decode(String body) {
        return java.net.URLDecoder.decode(body, StandardCharsets.UTF_8);
    }

    private void respond(HttpExchange exchange, int status, String json, long delayMillis)
            throws IOException {
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Test
    void shouldExchangeCodeWithAllOidcParameters() throws Exception {
        server.createContext("/token", exchange -> {
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, 200, """
                    {"access_token":"at","refresh_token":"rt","id_token":"idt","token_type":"Bearer","expires_in":300}
                    """, 0);
        });
        RestClientOidcTokenClient client = new RestClientOidcTokenClient(properties);

        OidcTokenClient.TokenResponse response =
                client.exchange(new OidcTokenClient.ExchangeRequest("the-code", "the-verifier"));

        assertEquals("at", response.accessToken());
        assertEquals("rt", response.refreshToken());
        assertEquals("idt", response.idToken());
        assertEquals(300, response.expiresInSeconds());
        String body = decode(lastBody);
        assertTrue(body.contains("grant_type=authorization_code"));
        assertTrue(body.contains("code=the-code"));
        assertTrue(body.contains("code_verifier=the-verifier"));
        assertTrue(body.contains("redirect_uri=http://localhost:8082/auth/callback"));
        assertTrue(body.contains("client_id=crm-gateway"));
    }

    @Test
    void shouldIncludeClientSecretWhenConfigured() throws Exception {
        properties.setClientSecret("secret");
        server.createContext("/token", exchange -> {
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, 200, """
                    {"access_token":"at","id_token":"idt","expires_in":300}
                    """, 0);
        });
        RestClientOidcTokenClient client = new RestClientOidcTokenClient(properties);

        client.exchange(new OidcTokenClient.ExchangeRequest("code", "verifier"));

        assertTrue(decode(lastBody).contains("client_secret=secret"));
    }

    @Test
    void shouldThrowOnTokenEndpointErrorStatus() throws Exception {
        server.createContext("/token", exchange ->
                respond(exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"nope\"}", 0));
        RestClientOidcTokenClient client = new RestClientOidcTokenClient(properties);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client.exchange(new OidcTokenClient.ExchangeRequest("code", "verifier")));

        assertEquals("TOKEN_EXCHANGE_FAILED", ex.getCode());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void shouldThrowWhenTokenResponseHasNoIdToken() throws Exception {
        server.createContext("/token", exchange ->
                respond(exchange, 200, "{\"access_token\":\"at\"}", 0));
        RestClientOidcTokenClient client = new RestClientOidcTokenClient(properties);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client.exchange(new OidcTokenClient.ExchangeRequest("code", "verifier")));

        assertEquals("TOKEN_RESPONSE_INVALID", ex.getCode());
    }

    @Test
    void shouldThrowOnTokenExchangeTimeout() throws Exception {
        properties.setTokenExchangeTimeout(Duration.ofMillis(300));
        server.createContext("/token", exchange ->
                respond(exchange, 200, "{\"access_token\":\"at\",\"id_token\":\"idt\"}", 2000));
        RestClientOidcTokenClient client = new RestClientOidcTokenClient(properties);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client.exchange(new OidcTokenClient.ExchangeRequest("code", "verifier")));

        assertEquals("TOKEN_EXCHANGE_TIMEOUT", ex.getCode());
    }

    @Test
    void shouldKeepRefreshTokenOptional() throws Exception {
        server.createContext("/token", exchange ->
                respond(exchange, 200, """
                        {"access_token":"at","id_token":"idt","expires_in":300}
                        """, 0));
        RestClientOidcTokenClient client = new RestClientOidcTokenClient(properties);

        OidcTokenClient.TokenResponse response =
                client.exchange(new OidcTokenClient.ExchangeRequest("code", "verifier"));

        assertNull(response.refreshToken());
    }

    // --------------------------------------------------------------- refresh

    private RestClientOidcTokenClient client() {
        return new RestClientOidcTokenClient(properties);
    }

    @Test
    void shouldRefreshTokensWithoutRedirectUri() throws Exception {
        properties.setClientSecret("secret");
        server.createContext("/token", exchange -> {
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, 200, """
                    {"access_token":"at2","refresh_token":"rt2","id_token":"idt2","expires_in":600}
                    """, 0);
        });

        OidcTokenClient.TokenResponse response =
                client().refresh(new OidcTokenClient.RefreshRequest("rt1"));

        assertEquals("at2", response.accessToken());
        assertEquals("rt2", response.refreshToken());
        assertEquals("idt2", response.idToken());
        assertEquals(600, response.expiresInSeconds());
        String body = decode(lastBody);
        assertTrue(body.contains("grant_type=refresh_token"));
        assertTrue(body.contains("refresh_token=rt1"));
        assertTrue(body.contains("client_id=crm-gateway"));
        assertTrue(body.contains("client_secret=secret"));
        assertFalse(body.contains("redirect_uri"), "refresh não deve enviar redirect_uri");
    }

    @Test
    void shouldThrowRefreshTokenInvalidOnInvalidGrant() throws Exception {
        server.createContext("/token", exchange ->
                respond(exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"expired\"}", 0));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client().refresh(new OidcTokenClient.RefreshRequest("rt")));

        assertEquals("REFRESH_TOKEN_INVALID", ex.getCode());
        assertEquals(401, ex.getStatus());
    }

    @Test
    void shouldThrowRefreshFailedOnOtherOidcError() throws Exception {
        server.createContext("/token", exchange ->
                respond(exchange, 400, "{\"error\":\"invalid_scope\"}", 0));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client().refresh(new OidcTokenClient.RefreshRequest("rt")));

        assertEquals("REFRESH_FAILED", ex.getCode());
    }

    @Test
    void shouldThrowProviderUnavailableOnRefreshTimeout() throws Exception {
        properties.setTokenExchangeTimeout(Duration.ofMillis(300));
        server.createContext("/token", exchange ->
                respond(exchange, 200, "{\"access_token\":\"at2\"}", 2000));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client().refresh(new OidcTokenClient.RefreshRequest("rt")));

        assertEquals("OIDC_PROVIDER_UNAVAILABLE", ex.getCode());
    }

    @Test
    void shouldThrowRefreshFailedWhenResponseHasNoAccessToken() throws Exception {
        server.createContext("/token", exchange ->
                respond(exchange, 200, "{\"id_token\":\"idt\"}", 0));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> client().refresh(new OidcTokenClient.RefreshRequest("rt")));

        assertEquals("REFRESH_FAILED", ex.getCode());
    }
}
