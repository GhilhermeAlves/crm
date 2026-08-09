package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.PendingLink;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPendingLinkStoreTest {

    private static final String TOKEN = "pending-token-1";

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final OidcGatewayProperties properties = new OidcGatewayProperties();
    private RedisPendingLinkStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        store = new RedisPendingLinkStore(redis, objectMapper, properties);
    }

    private PendingLink pendingLink(String token, Instant expiresAt) {
        Instant now = Instant.now();
        return new PendingLink(token, "sub-1", "local@example.com", "Ghilherme", "google",
                "csrf-1", "id-token", "access", "refresh", now.plusSeconds(300),
                "/dashboard", now, expiresAt);
    }

    private PendingLink activePendingLink(String token) {
        return pendingLink(token, Instant.now().plus(properties.getPendingLinkTtl()));
    }

    @Test
    void shouldStoreWithNativeTtlEqualToPendingLinkTtl() throws Exception {
        PendingLink pending = activePendingLink(TOKEN);
        store.put(pending);

        verify(valueOps).set(eq("gateway:pending-link:" + TOKEN), anyString(), eq(properties.getPendingLinkTtl()));
    }

    @Test
    void shouldRetrievePendingLinkWhenPresent() throws Exception {
        PendingLink pending = activePendingLink(TOKEN);
        store.put(pending);
        when(valueOps.get("gateway:pending-link:" + TOKEN))
                .thenReturn(objectMapper.writeValueAsString(pending));

        Optional<PendingLink> found = store.get(TOKEN);
        assertTrue(found.isPresent());
        assertEquals(TOKEN, found.get().token());
        assertEquals("local@example.com", found.get().email());
    }

    @Test
    void shouldReturnEmptyForNullOrUnknownToken() {
        when(valueOps.get("gateway:pending-link:unknown")).thenReturn(null);

        assertTrue(store.get("unknown").isEmpty());
        assertTrue(store.get(null).isEmpty());
    }

    @Test
    void shouldRemoveCorruptedValueAndReturnEmpty() {
        when(valueOps.get("gateway:pending-link:bad")).thenReturn("{corrupted");

        assertTrue(store.get("bad").isEmpty());
        verify(redis).delete("gateway:pending-link:bad");
    }

    @Test
    void shouldTreatLogicallyExpiredAsAbsentAndDelete() throws Exception {
        PendingLink expired = pendingLink(TOKEN, Instant.now().minusSeconds(1));
        store.put(expired);
        when(valueOps.get("gateway:pending-link:" + TOKEN))
                .thenReturn(objectMapper.writeValueAsString(expired));

        assertTrue(store.get(TOKEN).isEmpty());
        verify(redis).delete("gateway:pending-link:" + TOKEN);
    }

    @Test
    void shouldRemoveExplicitly() {
        store.remove(TOKEN);
        verify(redis).delete("gateway:pending-link:" + TOKEN);
    }

    @Test
    void shouldIgnoreNullOnRemove() {
        store.remove(null);
        verify(redis, never()).delete(anyString());
    }

    @Test
    void shouldPurgeBeNoOpRelyingOnNativeTtl() {
        store.purgeExpired();
        verify(valueOps, never()).set(anyString(), anyString());
        verify(redis, never()).delete(anyString());
    }

    @Test
    void shouldTranslateRedisFailureOnReadToDomainUnavailableError() {
        when(valueOps.get("gateway:pending-link:tX"))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> store.get("tX"));
        assertEquals("REDIS_UNAVAILABLE", ex.getCode());
        assertEquals(503, ex.getStatus());
        assertFalse(ex.getMessage().contains("refused"),
                "não deve vazar detalhes da exceção de infraestrutura");
    }

    @Test
    void shouldTranslateRedisFailureOnWriteToDomainUnavailableError() {
        org.mockito.Mockito.doThrow(new org.springframework.dao.QueryTimeoutException("timeout"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> store.put(activePendingLink("tY")));
        assertEquals("REDIS_UNAVAILABLE", ex.getCode());
        assertEquals(503, ex.getStatus());
    }

    @Test
    void shouldTranslateRedisFailureOnRemoveToDomainUnavailableError() {
        org.mockito.Mockito.doThrow(new RedisConnectionFailureException("down"))
                .when(redis).delete("gateway:pending-link:tZ");

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> store.remove("tZ"));
        assertEquals("REDIS_UNAVAILABLE", ex.getCode());
        assertEquals(503, ex.getStatus());
    }
}
