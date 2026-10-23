package com.becommerce.crm.masterdata.contact.registration.infrastructure.rate;

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
 * Limite por IP das rotas públicas do link de cadastro (20 requisições a cada 10
 * minutos). Mesmo mecanismo do {@code InvitationRateLimiter}: janela fixa em
 * Redis ({@code INCR} + {@code EXPIRE} atômicos), fail-open se o Redis cair.
 */
@Component
public class RegistrationRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RegistrationRateLimiter.class);

    static final int MAX_PER_WINDOW = 20;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "crm:ratelimit:registration:";

    private static final DefaultRedisScript<Long> INCR_EXPIRE = new DefaultRedisScript<>("""
            local count = redis.call("incr", KEYS[1])
            if count == 1 then
                redis.call("expire", KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public RegistrationRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean tryAcquire(String clientKey) {
        long windowSeconds = WINDOW.toSeconds();
        long bucketStart = Instant.now().getEpochSecond() / windowSeconds;
        String key = KEY_PREFIX + clientKey + ":" + bucketStart;
        Long count;
        try {
            count = redis.execute(INCR_EXPIRE, List.of(key), String.valueOf(windowSeconds));
        } catch (DataAccessException e) {
            log.warn("Registration rate limiter unavailable (Redis down), allowing request: error={}",
                    e.getClass().getSimpleName());
            return true;
        }
        return count == null || count <= MAX_PER_WINDOW;
    }
}
