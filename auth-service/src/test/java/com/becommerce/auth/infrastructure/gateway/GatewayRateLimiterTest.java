package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayRateLimiterTest {

    @Mock private StringRedisTemplate redis;

    @Test
    void shouldAllowRequestsWithinLimit() {
        AtomicLong counter = new AtomicLong();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.enforce("authorize", "ip-1", 5, Duration.ofSeconds(60)));
        }
    }

    @Test
    void shouldRejectWhenLimitExceeded() {
        AtomicLong counter = new AtomicLong();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);

        for (int i = 0; i < 5; i++) {
            limiter.enforce("refresh", "session-A", 5, Duration.ofSeconds(60));
        }
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> limiter.enforce("refresh", "session-A", 5, Duration.ofSeconds(60)));

        assertEquals(429, RateLimitExceededException.STATUS);
        assertEquals("RATE_LIMIT_EXCEEDED", RateLimitExceededException.CODE);
        assertTrue(ex.getRetryAfterSeconds() >= 1, "Retry-After deve ser >= 1s");
    }

    @Test
    void shouldNotBlockDifferentKeys() {
        Map<String, AtomicLong> counters = new java.util.concurrent.ConcurrentHashMap<>();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) invocation.getArgument(1);
                    return counters.computeIfAbsent(keys.get(0), k -> new AtomicLong()).incrementAndGet();
                });

        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);

        // sessão A excede o limite
        for (int i = 0; i < 5; i++) {
            limiter.enforce("refresh", "session-A", 5, Duration.ofSeconds(60));
        }
        assertThrows(RateLimitExceededException.class,
                () -> limiter.enforce("refresh", "session-A", 5, Duration.ofSeconds(60)));

        // sessão B continua permitida (contador independente por chave)
        for (int i = 0; i < 5; i++) {
            limiter.enforce("refresh", "session-B", 5, Duration.ofSeconds(60));
        }
        assertThrows(RateLimitExceededException.class,
                () -> limiter.enforce("refresh", "session-B", 5, Duration.ofSeconds(60)));
    }

    @Test
    void shouldNotBlockWhenRedisUnavailableFailControlled() {
        doThrow(new QueryTimeoutException("timeout"))
                .when(redis).execute(any(), anyList(), any(Object[].class));

        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);

        assertDoesNotThrow(() -> limiter.enforce("authorize", "ip-1", 5, Duration.ofSeconds(60)),
                "Redis fora deve permitir a requisição (fail-controlled)");
    }

    @Test
    void shouldNotBlockOnConnectionFailure() {
        doThrow(new RedisConnectionFailureException("Connection refused"))
                .when(redis).execute(any(), anyList(), any(Object[].class));

        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);

        assertDoesNotThrow(() -> limiter.enforce("callback", "ip-2", 5, Duration.ofSeconds(60)));
    }

    @Test
    void shouldBeDisabledWhenLimitIsZero() {
        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);

        assertDoesNotThrow(() -> limiter.enforce("authorize", "ip-1", 0, Duration.ofSeconds(60)),
                "limite 0 desativa o bucket sem tocar no Redis");
    }

    @Test
    void shouldHandleConcurrentRequests() throws Exception {
        AtomicLong counter = new AtomicLong();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        GatewayRateLimiter limiter = new GatewayRateLimiter(redis);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            Callable<Void> task = () -> {
                limiter.enforce("authorize", "ip-shared", 1000, Duration.ofSeconds(60));
                return null;
            };
            List<Future<Void>> futures = pool.invokeAll(java.util.Collections.nCopies(200, task));
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
        // todas as requisições concorrentes foram aceitas sem exceção
    }
}
