package com.becommerce.auth.presentation.rest.handler;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento global de erros no padrão do projeto:
 * {@code { status, code?, error, message, timestamp, correlationId? }}. O
 * {@code correlationId} é propagado do contexto da requisição (Sprint 6.6)
 * para que o erro do usuário seja rastreável nos logs — o header
 * {@code X-Correlation-Id} é emitido pelo {@code CorrelationIdFilter}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CrmAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleCrmAccessDenied(CrmAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(403, "CRM_ACCESS_DENIED", "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(OidcGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleOidcGateway(OidcGatewayException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(errorBody(ex.getStatus(), ex.getCode(),
                        HttpStatus.resolve(ex.getStatus()) == null ? "Error" : HttpStatus.resolve(ex.getStatus()).getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(RateLimitExceededException.STATUS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(errorBody(RateLimitExceededException.STATUS, RateLimitExceededException.CODE,
                        "Too Many Requests", ex.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(errorBody(405, "METHOD_NOT_ALLOWED", "Method Not Allowed",
                        "Método HTTP não permitido para este recurso."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, null, "Internal Server Error", "Erro interno inesperado."));
    }

    private Map<String, Object> errorBody(int status, String code, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        if (code != null) {
            body.put("code", code);
        }
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        String correlationId = CorrelationIdContext.get();
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        return body;
    }
}
