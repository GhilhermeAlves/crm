package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import com.becommerce.auth.infrastructure.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escreve a resposta {@code 429 Too Many Requests} no padrão do projeto
 * (Sprint 6.6), reutilizada por {@link GatewayRateLimitFilter} e
 * {@link ApiRateLimitFilter} para evitar duplicação da implementação de
 * correlation ID / formato de erro.
 *
 * <p>Corpo: {@code { status, code, error, message, timestamp, correlationId? }}
 * com headers {@code Retry-After} e {@code X-Correlation-Id} (o correlation ID
 * vem do {@link CorrelationIdContext} — nunca de dados sensíveis).
 */
@Component
public class RateLimitErrorResponse {

    private final ObjectMapper objectMapper;

    public RateLimitErrorResponse(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, RateLimitExceededException e) throws IOException {
        response.setStatus(RateLimitExceededException.STATUS);
        response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", RateLimitExceededException.STATUS);
        body.put("code", RateLimitExceededException.CODE);
        body.put("error", "Too Many Requests");
        body.put("message", e.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());
        String correlationId = CorrelationIdContext.get();
        if (correlationId != null) {
            response.setHeader(CorrelationIdFilter.HEADER, correlationId);
            body.put("correlationId", correlationId);
        }
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
