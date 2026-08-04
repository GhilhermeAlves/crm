package com.becommerce.auth.presentation.rest.handler;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tratamento global de erros no padrão do projeto:
 * {@code { status, code?, error, message, timestamp }}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CrmAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleCrmAccessDenied(CrmAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "code", "CRM_ACCESS_DENIED",
                "error", "Forbidden",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(OidcGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleOidcGateway(OidcGatewayException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "status", ex.getStatus(),
                "code", ex.getCode(),
                "error", HttpStatus.resolve(ex.getStatus()) == null ? "Error" : HttpStatus.resolve(ex.getStatus()).getReasonPhrase(),
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                "status", 405,
                "code", "METHOD_NOT_ALLOWED",
                "error", "Method Not Allowed",
                "message", "Método HTTP não permitido para este recurso.",
                "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Erro interno inesperado.",
                "timestamp", LocalDateTime.now().toString()));
    }
}
