package com.becommerce.auth.presentation.rest.handler;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldStandardizeOidcGatewayError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleOidcGateway(new OidcGatewayException("TOKEN_EXCHANGE_FAILED", 502, "falha no IdP"));

        assertEquals(502, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(502, body.get("status"));
        assertEquals("TOKEN_EXCHANGE_FAILED", body.get("code"));
        assertEquals("Bad Gateway", body.get("error"));
        assertEquals("falha no IdP", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void shouldStandardizeCrmAccessDeniedError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleCrmAccessDenied(new CrmAccessDeniedException("Usuário sem acesso ao CRM"));

        assertEquals(403, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("CRM_ACCESS_DENIED", body.get("code"));
        assertEquals("Forbidden", body.get("error"));
        assertEquals("Usuário sem acesso ao CRM", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void shouldReturn405WhenMethodNotAllowed() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMethodNotAllowed(new org.springframework.web.HttpRequestMethodNotSupportedException("GET"));

        assertEquals(405, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("METHOD_NOT_ALLOWED", body.get("code"));
        assertEquals("Method Not Allowed", body.get("error"));
        assertNotNull(body.get("message"));
    }

    @Test
    void shouldNotLeakExceptionDetailsOnUnexpectedError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new RuntimeException("senha-do-banco: hunter2, stack: com.becommerce.auth.X"));

        assertEquals(500, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("Erro interno inesperado.", body.get("message"));
        assertFalse(body.containsKey("code"), "erro genérico não deve expor código interno");
        assertTrue(body.values().stream().noneMatch(v -> String.valueOf(v).contains("hunter2")),
                "detalhes da exceção não devem vazar na resposta");
    }

    @Test
    void shouldReturn429WithRetryAfterOnRateLimitExceeded() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRateLimit(new RateLimitExceededException(37));

        assertEquals(429, response.getStatusCode().value());
        assertEquals("37", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("RATE_LIMIT_EXCEEDED", body.get("code"));
        assertEquals(429, body.get("status"));
        assertNotNull(body.get("message"));
    }

    @Test
    void shouldIncludeCorrelationIdInErrorBodyWhenPresent() {
        CorrelationIdContext.set("corr-12345678");
        try {
            ResponseEntity<Map<String, Object>> response =
                    handler.handleOidcGateway(new OidcGatewayException("TOKEN_EXCHANGE_FAILED", 502, "falha no IdP"));

            assertEquals("corr-12345678", response.getBody().get("correlationId"));
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void shouldOmitCorrelationIdWhenContextIsEmpty() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleOidcGateway(new OidcGatewayException("TOKEN_EXCHANGE_FAILED", 502, "falha no IdP"));

        assertFalse(response.getBody().containsKey("correlationId"));
    }
}
