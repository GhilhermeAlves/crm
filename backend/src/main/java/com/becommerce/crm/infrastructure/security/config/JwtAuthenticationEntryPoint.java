package com.becommerce.crm.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public JwtAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String message = authException != null && authException.getMessage() != null
                && !authException.getMessage().isBlank()
                ? authException.getMessage()
                : "Authentication is required to access this resource";

        if (authException instanceof CrmAccessDeniedAuthenticationException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getWriter(), Map.of(
                "status", 403,
                "code", "CRM_ACCESS_DENIED",
                "error", "Forbidden",
                "message", message,
                "timestamp", LocalDateTime.now().toString()
            ));
            return;
        }

        if (authException instanceof LinkingRequiredAuthenticationException) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getWriter(), Map.of(
                "status", 409,
                "code", "LINKING_REQUIRED",
                "error", "Conflict",
                "message", message,
                "timestamp", LocalDateTime.now().toString()
            ));
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        mapper.writeValue(response.getWriter(), Map.of(
            "status", 401,
            "error", "Unauthorized",
            "message", message,
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
