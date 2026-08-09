package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.PendingLink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Store distribuída dos vínculos pendentes (Sprint 7.2, Caso B) em Redis.
 * Ativada com {@code auth.gateway.session-store=redis}, na mesma decisão do
 * {@link RedisGatewaySessionStore}: múltiplas réplicas do auth-service
 * compartilham o estado e o vínculo sobrevive a reinícios.
 *
 * <p>Modelo de dados (somente o domínio serializável do {@link PendingLink}):
 * <ul>
 *   <li>{@code gateway:pending-link:<token>} → JSON do vínculo pendente.</li>
 * </ul>
 *
 * <p>Ciclo de vida:
 * <ul>
 *   <li><b>TTL nativo</b> do Redis igual ao {@code pendingLinkTtl} (10 min)
 *       como piso de expiração e <b>expiração lógica</b> na leitura — uma chave
 *       ainda presente além da expiração resolve ausente (e é removida);</li>
 *   <li><b>uso único</b> garantido pelo fluxo de {@code /auth/link}: o sucesso
 *       remove a chave (senha incorreta NÃO remove, permitindo retry);</li>
 *   <li>{@link #purgeExpired()} é no-op — o TTL nativo do Redis é a purga.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "auth.gateway.session-store", havingValue = "redis")
public class RedisPendingLinkStore implements PendingLinkStore {

    private static final Logger log = LoggerFactory.getLogger(RedisPendingLinkStore.class);

    private static final String KEY_PREFIX = "gateway:pending-link:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OidcGatewayProperties properties;

    public RedisPendingLinkStore(StringRedisTemplate redis,
                                 ObjectMapper objectMapper,
                                 OidcGatewayProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void put(PendingLink pendingLink) {
        redisRun(() -> redis.opsForValue().set(key(pendingLink.token()), serialize(pendingLink), ttl()));
    }

    @Override
    public Optional<PendingLink> get(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String json = redisOps(() -> redis.opsForValue().get(key(token)));
        if (json == null) {
            return Optional.empty();
        }
        PendingLink pendingLink;
        try {
            pendingLink = objectMapper.readValue(json, PendingLink.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize pending link, removing key={}", key(token));
            redisOps(() -> redis.delete(key(token)));
            return Optional.empty();
        }
        if (pendingLink.isExpired(Instant.now())) {
            redisOps(() -> redis.delete(key(token)));
            return Optional.empty();
        }
        return Optional.of(pendingLink);
    }

    @Override
    public void remove(String token) {
        if (token != null) {
            redisOps(() -> redis.delete(key(token)));
        }
    }

    /**
     * A purga de vínculos expirados é coberta pelo TTL nativo do Redis — mantida
     * idempotente por contrato, sem varredura.
     */
    @Override
    public void purgeExpired() {
        // no-op: o TTL nativo do Redis é o mecanismo de purga.
    }

    /**
     * Falhas de conexão/comando do Redis (todas {@link DataAccessException}) são
     * traduzidas para o erro de domínio {@code REDIS_UNAVAILABLE} (503), no mesmo
     * padrão do {@link RedisGatewaySessionStore} — a mensagem nunca vaza detalhes
     * de infraestrutura.
     */
    private <T> T redisOps(RedisOperation<T> operation) {
        try {
            return operation.execute();
        } catch (DataAccessException e) {
            log.warn("Redis unavailable during pending-link operation: {}", e.getClass().getSimpleName());
            throw new OidcGatewayException("REDIS_UNAVAILABLE", 503,
                    "Dependência de vínculo pendente indisponível (Redis).");
        }
    }

    private void redisRun(RedisAction action) {
        try {
            action.execute();
        } catch (DataAccessException e) {
            log.warn("Redis unavailable during pending-link operation: {}", e.getClass().getSimpleName());
            throw new OidcGatewayException("REDIS_UNAVAILABLE", 503,
                    "Dependência de vínculo pendente indisponível (Redis).");
        }
    }

    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute();
    }

    @FunctionalInterface
    private interface RedisAction {
        void execute();
    }

    private Duration ttl() {
        return properties.getPendingLinkTtl();
    }

    private String serialize(PendingLink pendingLink) {
        try {
            return objectMapper.writeValueAsString(pendingLink);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar vínculo pendente", e);
        }
    }

    private static String key(String token) {
        return KEY_PREFIX + token;
    }
}
