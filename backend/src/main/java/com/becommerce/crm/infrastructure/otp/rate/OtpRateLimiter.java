package com.becommerce.crm.infrastructure.otp.rate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Rate limiter distribuído para o fluxo de autenticação por telefone/OTP.
 * Mesma política do {@code InvitationRateLimiter}: janela fixa atômica via
 * Lua (INCR + EXPIRE) no Redis e fail-open controlado quando o Redis está
 * indisponível (nunca derruba o fluxo de login legítimo).
 */
@Component
public class OtpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(OtpRateLimiter.class);

    private static final int DEFAULT_MAX_SEND_PER_MINUTE = 5;
    private static final int DEFAULT_MAX_VERIFY_PER_MINUTE = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "crm:ratelimit:otp:";

    private static final DefaultRedisScript<Long> INCR_EXPIRE = new DefaultRedisScript<>("""
            local count = redis.call("incr", KEYS[1])
            if count == 1 then
                redis.call("expire", KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxSendPerWindow;
    private final int maxVerifyPerWindow;

    public OtpRateLimiter(StringRedisTemplate redis,
                          @Value("${app.otp.rate-limit.send-per-minute:" + DEFAULT_MAX_SEND_PER_MINUTE + "}") int maxSend,
                          @Value("${app.otp.rate-limit.verify-per-minute:" + DEFAULT_MAX_VERIFY_PER_MINUTE + "}") int maxVerify) {
        this.redis = redis;
        this.maxSendPerWindow = maxSend;
        this.maxVerifyPerWindow = maxVerify;
    }

    public boolean trySend(String clientIp) {
        return permit("send", clientIp, maxSendPerWindow);
    }

    public boolean tryVerify(String phoneE164) {
        return permit("verify", phoneE164, maxVerifyPerWindow);
    }

    private boolean permit(String bucket, String key, int max) {
        long windowSeconds = Math.max(WINDOW.toSeconds(), 1);
        long bucketStart = Instant.now().getEpochSecond() / windowSeconds;
        String redisKey = KEY_PREFIX + bucket + ":" + key + ":" + bucketStart;
        Long count;
        try {
            count = redis.execute(INCR_EXPIRE, List.of(redisKey), String.valueOf(windowSeconds));
        } catch (Exception e) {
            log.warn("OTP rate limiter unavailable (Redis down), allowing request: bucket={} error={}",
                    bucket, e.getClass().getSimpleName());
            return true;
        }
        return count == null || count <= max;
    }
}