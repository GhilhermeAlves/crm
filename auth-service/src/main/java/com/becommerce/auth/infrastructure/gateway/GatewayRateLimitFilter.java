package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting do Access Gateway (Sprint 6.6). Protege os endpoints sensíveis
 * de autenticação — {@code /auth/authorize}, {@code /auth/callback},
 * {@code /auth/refresh} e {@code /auth/logout} — com contador distribuído em
 * Redis ({@link GatewayRateLimiter}).
 *
 * <p>Chave por endpoint:
 * <ul>
 *   <li>{@code authorize}/{@code callback} (sem sessão ainda) → IP real do
 *       cliente ({@link ClientIpResolver} — resolve respeitando a cadeia de
 *       proxies e ignorando spoofing de {@code X-Forwarded-For});</li>
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
    private final ClientIpResolver clientIpResolver;
    private final RateLimitErrorResponse errorResponse;
    private final OidcGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayRateLimitFilter(GatewayRateLimiter rateLimiter,
                                  GatewayCookieFactory cookieFactory,
                                  ClientIpResolver clientIpResolver,
                                  RateLimitErrorResponse errorResponse,
                                  OidcGatewayProperties properties,
                                  ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.cookieFactory = cookieFactory;
        this.clientIpResolver = clientIpResolver;
        this.errorResponse = errorResponse;
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
            errorResponse.write(response, e);
        }
    }

    private RateLimitTarget resolveTarget(HttpServletRequest request) {
        return switch (request.getRequestURI()) {
            case "/auth/authorize" -> new RateLimitTarget(
                    "authorize", clientIpResolver.resolve(request), properties.getRateLimitAuthorize());
            case "/auth/callback" -> new RateLimitTarget(
                    "callback", clientIpResolver.resolve(request), properties.getRateLimitCallback());
            case "/auth/refresh" -> new RateLimitTarget(
                    "refresh", sessionKeyOrIp(request), properties.getRateLimitRefresh());
            case "/auth/logout" -> new RateLimitTarget(
                    "logout", sessionKeyOrIp(request), properties.getRateLimitLogout());
            default -> null;
        };
    }

    private String sessionKeyOrIp(HttpServletRequest request) {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        return sessionToken != null ? sessionToken : clientIpResolver.resolve(request);
    }

    private record RateLimitTarget(String bucket, String key, int limit) {
    }
}
