package com.becommerce.auth.infrastructure.config;

import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita a configuração do Access Gateway OIDC ({@code auth.gateway.*}) e o
 * agendamento da purga de estados/sessões em memória.
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
}
