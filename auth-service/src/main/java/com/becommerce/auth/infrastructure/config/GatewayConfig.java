package com.becommerce.auth.infrastructure.config;

import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
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
}
