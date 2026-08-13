package com.becommerce.crm.infrastructure.invitation.rate;

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
 * Rate limiter distribuído para convites (Sprint 8.5), agora com janela fixa em
 * Redis (Sprint 10 follow-up) — substitui a implementação em memória para que o
 * limite valha mesmo em cenário multi-instância.
 *
 * <p>O contador é atômico ({@code INCR} + {@code EXPIRE} no primeiro incremento
 * via Lua), então o limite é compartilhado por todas as instâncias do backend.
 * Chaves com TTL nativo dispensam limpeza manual (removeu {@code prune()}).
 *
 * <p><b>Política de falha (fail-open controlado):</b> quando o Redis está
 * indisponível a requisição é <b>permitida</b> com warning (mesma política do
 * {@code GatewayRateLimiter} do auth-service) — o limiter nunca derruba o
 * backend nem bloqueia convites legítimos durante indisponibilidade.
 *
 * <p>Limita a criação de convites por empresa ({@code MAX_CREATES_PER_WINDOW}) e
 * as tentativas de aceite por usuário ({@code MAX_ACCEPTS_PER_WINDOW}), reduzindo
 * abuso/força bruta de token.
 */
@Component
public class InvitationRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InvitationRateLimiter.class);

    private static final int MAX_CREATES_PER_WINDOW = 20;
    private static final int MAX_ACCEPTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(60);

    private static final String KEY_PREFIX = "crm:ratelimit:invitations:";

    private static final DefaultRedisScript<Long> INCR_EXPIRE = new DefaultRedisScript<>("""
            local count = redis.call("incr", KEYS[1])
            if count == 1 then
                redis.call("expire", KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public InvitationRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean tryCreate(String companyId) {
        return permit("create", companyId, MAX_CREATES_PER_WINDOW);
    }

    public boolean tryAccept(String key) {
        return permit("accept", key, MAX_ACCEPTS_PER_WINDOW);
    }

    private boolean permit(String bucket, String key, int max) {
        long windowSeconds = Math.max(WINDOW.toSeconds(), 1);
        long bucketStart = Instant.now().getEpochSecond() / windowSeconds;
        String redisKey = KEY_PREFIX + bucket + ":" + key + ":" + bucketStart;

        Long count;
        try {
            count = redis.execute(INCR_EXPIRE, List.of(redisKey), String.valueOf(windowSeconds));
        } catch (DataAccessException e) {
            log.warn("Invitation rate limiter unavailable (Redis down), allowing request: bucket={} error={}",
                    bucket, e.getClass().getSimpleName());
            return true;
        }

        return count == null || count <= max;
    }
}
