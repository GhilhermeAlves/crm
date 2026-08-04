package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.becommerce.auth.domain.gateway.SessionStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting do relay {@code /api/*} (Sprint 6.7). Roda <b>antes</b> do
 * {@link ApiRelayController} encaminhar a requisição ao backend.
 *
 * <p>Estratégia da chave do bucket (documentada no report 6.7):
 * <ul>
 *   <li>identidade principal: {@code userId} (UUID) da sessão autenticada
 *       ({@link GatewaySession}) — resolvida no servidor, opaca, nunca
 *       controlável pelo cliente (nem via cookie, nem via headers);</li>
 *   <li>fallback: IP real do cliente ({@link ClientIpResolver}, seguro contra
 *       spoofing de {@code X-Forwarded-For});</li>
 *   <li>bucket único por identidade para todo {@code /api/*} na janela
 *       configurada — previsível e de baixo custo Redis (1 chave por usuário
 *       ativo por janela); buckets por rota podem ser adicionados sem mudar o
 *       mecanismo.</li>
 * </ul>
 *
 * <p>Limite excedido → {@code 429} JSON via {@link RateLimitErrorResponse}
 * (mesmo padrão da Sprint 6.6, com {@code Retry-After} e
 * {@code X-Correlation-Id}). Redis indisponível: identidade cai para IP e o
 * {@link GatewayRateLimiter} permanece fail-controlled (não bloqueia o tráfego
 * quando o Redis está fora). Nenhum token/sessão é logado.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);

    private final GatewayRateLimiter rateLimiter;
    private final GatewayCookieFactory cookieFactory;
    private final GatewaySessionResolver sessionResolver;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitErrorResponse errorResponse;
    private final OidcGatewayProperties properties;

    public ApiRateLimitFilter(GatewayRateLimiter rateLimiter,
                              GatewayCookieFactory cookieFactory,
                              GatewaySessionResolver sessionResolver,
                              ClientIpResolver clientIpResolver,
                              RateLimitErrorResponse errorResponse,
                              OidcGatewayProperties properties) {
        this.rateLimiter = rateLimiter;
        this.cookieFactory = cookieFactory;
        this.sessionResolver = sessionResolver;
        this.clientIpResolver = clientIpResolver;
        this.errorResponse = errorResponse;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!properties.isRateLimitEnabled() || properties.getRateLimitApi() <= 0) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = resolveKey(request);
        try {
            rateLimiter.enforce("api", key, properties.getRateLimitApi(), properties.getRateLimitWindow());
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException e) {
            errorResponse.write(response, e);
        }
    }

    private String resolveKey(HttpServletRequest request) {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        if (sessionToken != null) {
            String userId = resolveAuthenticatedUser(sessionToken);
            if (userId != null) {
                return userId;
            }
        }
        return clientIpResolver.resolve(request);
    }

    private String resolveAuthenticatedUser(String sessionToken) {
        try {
            SessionLookup lookup = sessionResolver.resolve(sessionToken);
            if (lookup.status() == SessionStatus.ACTIVE) {
                return lookup.session().userId().toString();
            }
        } catch (OidcGatewayException e) {
            // Redis indisponível → identidade desconhecida; cai para IP e o
            // próprio rate limiter permanece fail-controlled (permite com
            // warning, nunca derruba o gateway).
            log.debug("API rate limit: identity resolution unavailable, falling back to IP: error={}",
                    e.getCode());
        }
        return null;
    }
}
