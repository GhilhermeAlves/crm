package com.becommerce.crm.infrastructure.invitation.rate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationRateLimiterTest {

    @Mock private StringRedisTemplate redis;

    @Test
    void shouldAllowCreatesWithinLimit() {
        AtomicLong counter = new AtomicLong();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        InvitationRateLimiter limiter = new InvitationRateLimiter(redis);

        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.tryCreate("company-1"), "20 criações dentro do limite devem ser permitidas");
        }
    }

    @Test
    void shouldRejectCreateWhenLimitExceeded() {
        AtomicLong counter = new AtomicLong();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        InvitationRateLimiter limiter = new InvitationRateLimiter(redis);

        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.tryCreate("company-1"));
        }
        assertFalse(limiter.tryCreate("company-1"), "21ª criação deve ser bloqueada (20/h)");
    }

    @Test
    void shouldRejectAcceptWhenLimitExceeded() {
        AtomicLong counter = new AtomicLong();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> counter.incrementAndGet());

        InvitationRateLimiter limiter = new InvitationRateLimiter(redis);

        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAccept("user-1"));
        }
        assertFalse(limiter.tryAccept("user-1"), "11ª tentativa de aceite deve ser bloqueada (10/h)");
    }

    @Test
    void shouldKeepCountersIndependentByKey() {
        Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) invocation.getArgument(1);
                    return counters.computeIfAbsent(keys.get(0), k -> new AtomicLong()).incrementAndGet();
                });

        InvitationRateLimiter limiter = new InvitationRateLimiter(redis);

        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.tryCreate("company-A"));
        }
        assertFalse(limiter.tryCreate("company-A"));

        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.tryCreate("company-B"), "empresa B mantém contador independente");
        }
    }

    @Test
    void shouldFailOpenWhenRedisUnavailable() {
        doThrow(new QueryTimeoutException("timeout"))
                .when(redis).execute(any(), anyList(), any(Object[].class));

        InvitationRateLimiter limiter = new InvitationRateLimiter(redis);

        assertTrue(limiter.tryCreate("company-1"), "Redis fora deve permitir (fail-open controlado)");
        assertTrue(limiter.tryAccept("user-1"));
    }

    @Test
    void shouldFailOpenOnConnectionFailure() {
        doThrow(new RedisConnectionFailureException("Connection refused"))
                .when(redis).execute(any(), anyList(), any(Object[].class));

        InvitationRateLimiter limiter = new InvitationRateLimiter(redis);

        assertTrue(limiter.tryCreate("company-1"));
    }
}
