package com.becommerce.crm.infrastructure.otp.rate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpRateLimiterTest {

    private static final int MAX_SEND = 5;       // por IP a cada minuto
    private static final int MAX_VERIFY = 5;     // por telefone a cada minuto

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private OtpRateLimiter limiter;

    @BeforeEach
    void setup() {
        limiter = new OtpRateLimiter(redis, MAX_SEND, MAX_VERIFY);
    }

    @Test
    void primeiraChamada_permite() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenReturn(1L);
        assertTrue(limiter.trySend("1.2.3.4"));
    }

    @Test
    void acimaDoLimite_deveBloquear() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenReturn((long) (MAX_SEND + 1));
        assertFalse(limiter.trySend("1.2.3.4"));
    }

    @Test
    void verificar_acimaDoLimite_deveBloquear() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenReturn((long) (MAX_VERIFY + 1));
        assertFalse(limiter.tryVerify("+5511999999999"));
    }

    @Test
    void redisIndisponivel_permitePorFailOpen() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenThrow(new RuntimeException("redis down"));
        assertTrue(limiter.trySend("1.2.3.4"));
    }
}