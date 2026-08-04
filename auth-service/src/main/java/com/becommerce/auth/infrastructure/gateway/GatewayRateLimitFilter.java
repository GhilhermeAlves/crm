package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import com.becommerce.auth.infrastructure.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate limiting do Access Gateway (Sprint 6.6). Protege os endpoints sensíveis
 * de autenticação — {@code /auth/authorize}, {@code /auth/callback},
 * {@code /auth/refresh} e {@code /auth/logout} — com contador distribuído em
 * Redis ({@link GatewayRateLimiter}).
 *
 * <p>Chave por endpoint:
 * <ul>
 *   <li>{@code authorize}/{@code callback} (sem sessão ainda) → IP real do
 *       cliente (nginx sobrescreve {@code X-Forwarded-For} com
 *       {@code $remote_addr});</li>
 *   <li>{@code refresh}/{@code logout} → {@code sessionToken} opaco do cookie
 *       (sessões diferentes não se bloqueiam) com fallback para IP quando não
 *       há sessão.</li>
 * </ul>
 *
 * <p>Limite excedido → {@code 429} JSON no padrão do projeto com
 * {@code Retry-After} e {@code X-Correlation-Id}; o fluxo legítimo (dentro do
 * limite) não é afetado.
 */
public class GatewayRateLimitFilter extends OncePerRequestFilter {

    private final GatewayRateLimiter rateLimiter;
    private final GatewayCookieFactory cookieFactory;
    private final OidcGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayRateLimitFilter(GatewayRateLimiter rateLimiter,
                                  GatewayCookieFactory cookieFactory,
                                  OidcGatewayProperties properties,
                                  ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.cookieFactory = cookieFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitTarget target = resolveTarget(request);
        if (target == null || !properties.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            rateLimiter.enforce(target.bucket(), target.key(), target.limit(), properties.getRateLimitWindow());
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException e) {
            writeTooManyRequests(response, e);
        }
    }

    private RateLimitTarget resolveTarget(HttpServletRequest request) {
        return switch (request.getRequestURI()) {
            case "/auth/authorize" -> new RateLimitTarget(
                    "authorize", clientIp(request), properties.getRateLimitAuthorize());
            case "/auth/callback" -> new RateLimitTarget(
                    "callback", clientIp(request), properties.getRateLimitCallback());
            case "/auth/refresh" -> new RateLimitTarget(
                    "refresh", sessionKeyOrIp(request), properties.getRateLimitRefresh());
            case "/auth/logout" -> new RateLimitTarget(
                    "logout", sessionKeyOrIp(request), properties.getRateLimitLogout());
            default -> null;
        };
    }

    private String sessionKeyOrIp(HttpServletRequest request) {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        return sessionToken != null ? sessionToken : clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String first = forwardedFor.split(",", 2)[0].trim();
            if (isPlausibleIp(first)) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp) && isPlausibleIp(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private boolean isPlausibleIp(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.matches("\\d{1,3}(\\.\\d{1,3}){3}") || value.matches("[0-9a-fA-F:]+");
    }

    private void writeTooManyRequests(HttpServletResponse response, RateLimitExceededException e) throws IOException {
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

    private record RateLimitTarget(String bucket, String key, int limit) {
    }
}
