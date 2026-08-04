package com.becommerce.auth.infrastructure.gateway;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OidcProviderMetadataTest {

    private HttpServer server;
    private String baseUrl;
    private AtomicInteger discoveryHits;
    private OidcGatewayProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
        discoveryHits = new AtomicInteger();
        properties = new OidcGatewayProperties();
        properties.setIssuerUri(baseUrl + "/realms/CRM");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Test
    void shouldReturnEndSessionEndpointFromDiscovery() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange -> {
            discoveryHits.incrementAndGet();
            respond(exchange, 200, """
                    {"issuer":"%s/realms/CRM","end_session_endpoint":"%s/realms/CRM/protocol/openid-connect/logout"}
                    """.formatted(baseUrl, baseUrl));
        });

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);

        assertEquals(baseUrl + "/realms/CRM/protocol/openid-connect/logout", metadata.endSessionEndpoint());
    }

    @Test
    void shouldCacheEndpointBetweenCalls() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange -> {
            discoveryHits.incrementAndGet();
            respond(exchange, 200, """
                    {"issuer":"%s","end_session_endpoint":"%s/logout"}
                    """.formatted(baseUrl, baseUrl));
        });

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);
        metadata.endSessionEndpoint();
        metadata.endSessionEndpoint();

        assertEquals(1, discoveryHits.get(), "descoberta deve ser cacheada por TTL");
    }

    @Test
    void shouldThrowProviderUnavailableWhenDiscoveryFails() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange ->
                respond(exchange, 404, "{}"));

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, metadata::endSessionEndpoint);
        assertEquals("OIDC_PROVIDER_UNAVAILABLE", ex.getCode());
    }

    @Test
    void shouldThrowProviderUnavailableWhenEndpointNotPublished() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange ->
                respond(exchange, 200, "{\"issuer\":\"%s\"}".formatted(baseUrl)));

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);

        assertThrows(OidcGatewayException.class, metadata::endSessionEndpoint);
    }

    @Test
    void isReachableShouldReturnTrueWhenDiscoverySucceeds() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange -> {
            discoveryHits.incrementAndGet();
            respond(exchange, 200, "{\"issuer\":\"%s\"}".formatted(baseUrl));
        });

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);

        assertTrue(metadata.isReachable());
    }

    @Test
    void isReachableShouldReturnFalseWhenDiscoveryFails() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange ->
                respond(exchange, 503, "{}"));

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);

        assertFalse(metadata.isReachable(), "indisponibilidade do provedor não deve lançar, apenas reportar");
    }

    @Test
    void isReachableShouldRecoverAfterFailure() throws Exception {
        server.createContext("/realms/CRM/.well-known/openid-configuration", exchange -> {
            if (discoveryHits.getAndIncrement() == 0) {
                respond(exchange, 503, "{}");
            } else {
                respond(exchange, 200, "{\"issuer\":\"%s\"}".formatted(baseUrl));
            }
        });

        OidcProviderMetadata metadata = new OidcProviderMetadata(properties);

        assertFalse(metadata.isReachable());
        assertTrue(metadata.isReachable(), "recuperação do provedor deve ser refletida na próxima sondagem");
    }
}
