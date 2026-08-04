package com.becommerce.auth.infrastructure.health;

import com.becommerce.auth.infrastructure.gateway.OidcProviderMetadata;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Sondagem de dependências para o readiness do Access Gateway (Sprint 6.6).
 *
 * <p>Nunca expõe detalhes administrativos (host/porta/segredo/stack): apenas
 * informa se cada dependência responde ({@code true/false}). As dependências
 * avaliadas são as críticas para operar o Gateway:
 * <ul>
 *   <li><b>Redis</b> — store de sessão/estado ({@code PING});</li>
 *   <li><b>Keycloak/OIDC</b> — descoberta {@code /.well-known/
 *       openid-configuration} (mesmo mecanismo de discovery do logout).</li>
 * </ul>
 * O banco relacional (CRM) é coberto pelo actuator {@code /actuator/health}
 * (health do banco), não duplicado aqui para não adicionar dependência
 * artificial ao liveness.
 */
@Component
public class DependencyProbe {

    private final StringRedisTemplate redis;
    private final OidcProviderMetadata providerMetadata;

    public DependencyProbe(StringRedisTemplate redis, OidcProviderMetadata providerMetadata) {
        this.redis = redis;
        this.providerMetadata = providerMetadata;
    }

    public boolean redisReachable() {
        try {
            String pong = redis.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(pong);
        } catch (DataAccessException e) {
            return false;
        }
    }

    public boolean keycloakReachable() {
        return providerMetadata.isReachable();
    }
}
