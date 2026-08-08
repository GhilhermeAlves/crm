package com.becommerce.auth.infrastructure.security;

import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Protege o endpoint interno de resgate de credencial (Sprint 7.4):
 * {@code POST /internal/auth/reset-password}. Diferente de
 * {@code /internal/auth/current-user} (autenticado via JWT do usuário no
 * Keycloak), o reset de senha é chamado pelo crm-backend de serviço a serviço,
 * sem sessão de usuário — a autenticação é um segredo compartilhado
 * ({@code auth.internal.api-token}, header {@code X-Internal-Api-Token}) definido
 * por ambiente ({@code AUTH_INTERNAL_API_TOKEN}).
 *
 * <p>O filtro só aplica ao path do endpoint (via {@code FilterRegistrationBean});
 * para outros paths a requisição segue a cadeia normalmente. Segredo ausente ou
 * divergente → 401 JSON no padrão do projeto (sem correlation ID sensível).
 *
 * <p>O bean é criado em {@code GatewayConfig#internalApiTokenFilter} (não é
 * {@code @Component}) para garantir registro ordenado antes do Spring Security,
 * no mesmo padrão do {@code ApiRateLimitFilter}.
 */
public class InternalApiTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Token";

    private final String apiToken;
    private final ObjectMapper objectMapper;

    public InternalApiTokenFilter(@Value("${auth.internal.api-token:}") String apiToken,
                                  ObjectMapper objectMapper) {
        this.apiToken = apiToken == null ? "" : apiToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().toLowerCase(Locale.ROOT).endsWith("/internal/auth/reset-password");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (apiToken.isBlank() || provided == null || !apiToken.equals(provided)) {
            writeUnauthorized(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 401);
        body.put("code", "INTERNAL_API_TOKEN_INVALID");
        body.put("error", "Unauthorized");
        body.put("message", "Token de API interna ausente ou inválido.");
        body.put("timestamp", LocalDateTime.now().toString());
        String correlationId = CorrelationIdContext.get();
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}