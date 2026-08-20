package com.becommerce.crm.presentation.rest.handler;

import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapQuotaExceededTo422WithCode() {
        var response = handler.handleQuotaExceededException(
                new QuotaExceededException("Limite de usuários da empresa atingido (5)."));

        assertEquals(422, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertEquals(422, body.get("status"));
        assertEquals("QUOTA_EXCEEDED", body.get("code"));
        assertEquals("Limite de usuários da empresa atingido (5).", body.get("message"));
    }

    @Test
    void shouldMapAccessDeniedTo403() {
        var response = handler.handleCrmAccessDenied(
                new com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException("Sem acesso."));
        assertEquals(403, response.getStatusCode().value());
        assertEquals("CRM_ACCESS_DENIED", response.getBody().get("code"));
    }

    @Test
    void shouldMapOmnichannelNotFoundTo404() {
        var response = handler.handleOmnichannelNotFoundException(
                new com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException(
                        java.util.UUID.randomUUID(), "Conversa"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
    }
}