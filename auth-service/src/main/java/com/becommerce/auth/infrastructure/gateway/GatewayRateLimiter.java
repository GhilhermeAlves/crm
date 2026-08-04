package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Rate limiter distribuído do Access Gateway (Sprint 6.6), janela fixa em Redis.
 *
 * <p>O contador é atômico ({@code INCR} + {@code EXPIRE} no primeiro incremento
 * via Lua), portanto o limite vale mesmo com múltiplas instâncias do
 * auth-service — nenhum contador local/memória é usado em produção.
 *
 * <p><b>Política de falha do Redis (fail-controlled):</b> quando o Redis está
 * indisponível, a requisição <b>é permitida</b> (fail-open controlado) com um
 * warning no log. Justificativa: as operações que o rate limiter protege
 * (authorize/callback/refresh/logout) já dependem de Redis para sessão/estado e
 * falham de forma segura (503 REDIS_UNAVAILABLE / 401) quando ele cai; bloquear
 * (fail-closed) durante a indisponibilidade negaria o acesso legítimo a todos os
 * usuários sem proteção adicional — o limiter nunca derruba o auth-service.
 */
@Component
public class GatewayRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(GatewayRateLimiter.class);

    private static final String KEY_PREFIX = "gateway:ratelimit:";

    private static final DefaultRedisScript<Long> INCR_WINDOW = new DefaultRedisScript<>("""
            local count = redis.call("incr", KEYS[1])
            if count == 1 then
                redis.call("expire", KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public GatewayRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Registra a requisição no bucket e lança {@link RateLimitExceededException}
     * quando o limite da janela é ultrapassado.
     *
     * @param bucket  categoria (ex.: {@code authorize}, {@code refresh})
     * @param key     chave do bucket (IP real do cliente ou sessionToken opaco)
     * @param limit   máximo de requisições por janela (0 desativa o bucket)
     * @param window  tamanho da janela fixa
     */
    public void enforce(String bucket, String key, int limit, Duration window) {
        if (limit <= 0) {
            return;
        }
        long windowSeconds = Math.max(window.toSeconds(), 1);
        long windowStart = Instant.now().getEpochSecond() / windowSeconds;
        String redisKey = KEY_PREFIX + bucket + ":" + key + ":" + windowStart;

        Long count;
        try {
            count = redis.execute(INCR_WINDOW, List.of(redisKey), String.valueOf(windowSeconds));
        } catch (DataAccessException e) {
            log.warn("Rate limiter unavailable (Redis down), allowing request: bucket={} error={}",
                    bucket, e.getClass().getSimpleName());
            return;
        }

        if (count != null && count > limit) {
            long windowEnd = (windowStart + 1) * windowSeconds;
            long retryAfter = Math.max(windowEnd - Instant.now().getEpochSecond(), 1);
            throw new RateLimitExceededException(retryAfter);
        }
    }
}
