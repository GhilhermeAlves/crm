package com.becommerce.auth.infrastructure.config;

import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.application.gateway.service.ConfiguredIdentityProviderCatalog;
import com.becommerce.auth.infrastructure.gateway.ApiRateLimitFilter;
import com.becommerce.auth.infrastructure.gateway.ClientIpResolver;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.gateway.GatewayRateLimiter;
import com.becommerce.auth.infrastructure.gateway.GatewayRateLimitFilter;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionResolver;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.becommerce.auth.infrastructure.gateway.RateLimitErrorResponse;
import com.becommerce.auth.infrastructure.gateway.SecureTokenGenerator;
import com.becommerce.auth.infrastructure.observability.CorrelationIdFilter;
import com.becommerce.auth.infrastructure.security.GatewayCsrfFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * Habilita a configuração do Access Gateway OIDC ({@code auth.gateway.*}), o
 * agendamento da purga de estados/sessões em memória e registra o
 * {@link GatewayCsrfFilter} (proteção cookie-to-header de {@code /auth/refresh}).
 *
 * <p>Validação de segurança: no profile {@code prod} o cookie de sessão do
 * gateway é obrigatoriamente {@code Secure}. Configuração insegura
 * ({@code auth.gateway.secure-cookie=false}) em produção falha o startup —
 * nunca de forma silenciosa.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OidcGatewayProperties.class)
public class GatewayConfig {

    public GatewayConfig(OidcGatewayProperties properties, Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod")) && !properties.isSecureCookie()) {
            throw new IllegalStateException(
                    "auth.gateway.secure-cookie=false não é permitido no profile prod: o cookie de sessão deve ser Secure.");
        }
    }

    /**
     * Catálogo de provedores de identidade (Sprint 7.0): alimenta
     * {@code GET /auth/providers} e o {@code kc_idp_hint} do
     * {@code /auth/authorize}. Disponível 100% no servidor; o browser não
     * decide provedores nem buckets de rate limit.
     */
    @Bean
    IdentityProviderCatalog identityProviderCatalog(OidcGatewayProperties properties) {
        return new ConfiguredIdentityProviderCatalog(properties);
    }

    @Bean
    FilterRegistrationBean<GatewayCsrfFilter> gatewayCsrfFilter(GatewayCookieFactory cookieFactory,
                                                                OidcGatewayProperties properties,
                                                                ObjectMapper objectMapper) {
        FilterRegistrationBean<GatewayCsrfFilter> registration =
                new FilterRegistrationBean<>(new GatewayCsrfFilter(cookieFactory, properties, objectMapper));
        registration.setUrlPatterns(List.of("/auth/refresh"));
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return registration;
    }

    /**
     * {@link CorrelationIdFilter} (Sprint 6.6): roda antes de toda a cadeia
     * (inclusive Spring Security) para que toda requisição — incluindo erros
     * 401/403/429 — tenha {@code X-Correlation-Id} na resposta e no log.
     */
    @Bean
    FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(SecureTokenGenerator tokenGenerator) {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter(tokenGenerator));
        registration.setUrlPatterns(List.of("/*"));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * {@link GatewayRateLimitFilter} (Sprint 6.6): limita os endpoints sensíveis
     * de autenticação com contador distribuído em Redis. Executa antes do
     * Spring Security (limita também requisições anônimas).
     */
    @Bean
    FilterRegistrationBean<GatewayRateLimitFilter> gatewayRateLimitFilter(GatewayRateLimiter rateLimiter,
                                                                          GatewayCookieFactory cookieFactory,
                                                                          ClientIpResolver clientIpResolver,
                                                                          RateLimitErrorResponse errorResponse,
                                                                          OidcGatewayProperties properties,
                                                                          ObjectMapper objectMapper) {
        FilterRegistrationBean<GatewayRateLimitFilter> registration =
                new FilterRegistrationBean<>(new GatewayRateLimitFilter(
                        rateLimiter, cookieFactory, clientIpResolver, errorResponse, properties, objectMapper));
        registration.setUrlPatterns(List.of("/auth/authorize", "/auth/callback", "/auth/refresh", "/auth/logout", "/auth/link"));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * {@link ApiRateLimitFilter} (Sprint 6.7): limita o relay {@code /api/**}
     * por usuário autenticado ({@code userId} da sessão) com fallback para IP
     * real — antes do encaminhamento ao backend.
     */
    @Bean
    FilterRegistrationBean<ApiRateLimitFilter> apiRateLimitFilter(GatewayRateLimiter rateLimiter,
                                                                  GatewayCookieFactory cookieFactory,
                                                                  GatewaySessionResolver sessionResolver,
                                                                  ClientIpResolver clientIpResolver,
                                                                  RateLimitErrorResponse errorResponse,
                                                                  OidcGatewayProperties properties) {
        FilterRegistrationBean<ApiRateLimitFilter> registration =
                new FilterRegistrationBean<>(new ApiRateLimitFilter(
                        rateLimiter, cookieFactory, sessionResolver, clientIpResolver, errorResponse, properties));
        registration.setUrlPatterns(List.of("/api/*"));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
