package com.becommerce.auth.infrastructure.security;

import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Proteção CSRF cookie-to-header para endpoints mutáveis autenticados pelo
 * cookie de sessão do gateway ({@code POST /auth/refresh}) — Sprint 6.2.
 *
 * <p>O browser envia o token CSRF no cookie (legível por JS) e o mesmo valor no
 * header {@code X-XSRF-TOKEN}; o filtro compara os dois. Requisições GET e
 * endpoints que não são o {@code /auth/refresh} não são interceptados.
 *
 * <p>Registrado via {@code FilterRegistrationBean} (URL {@code /auth/refresh},
 * depois do chain do Spring Security), pois o CSRF default está desabilitado.
 */
public class GatewayCsrfFilter implements Filter {

    private final GatewayCookieFactory cookieFactory;
    private final OidcGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayCsrfFilter(GatewayCookieFactory cookieFactory,
                             OidcGatewayProperties properties,
                             ObjectMapper objectMapper) {
        this.cookieFactory = cookieFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!isProtected(request)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        String cookieToken = cookieFactory.readCsrfToken(request.getCookies()).orElse(null);
        String headerToken = request.getHeader(properties.getCsrfHeaderName());
        if (cookieToken == null || !cookieToken.equals(headerToken)) {
            reject(response);
            return;
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    private boolean isProtected(HttpServletRequest request) {
        return request.getRequestURI().equals("/auth/refresh")
                && request.getMethod().equalsIgnoreCase(HttpMethod.POST.name());
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", 403,
                "code", "CSRF_INVALID",
                "error", "Forbidden",
                "message", "Token CSRF ausente ou inválido.",
                "timestamp", LocalDateTime.now().toString()));
    }
}
