package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.SessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisGatewaySessionStoreTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final OidcGatewayProperties properties = new OidcGatewayProperties();
    private RedisGatewaySessionStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        store = new RedisGatewaySessionStore(redis, objectMapper, properties);
    }

    private GatewaySession session(String token, Instant createdAt, Instant expiresAt,
                                   Instant lastAccessedAt, Instant revokedAt) {
        return new GatewaySession(token, USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "A", createdAt, expiresAt,
                lastAccessedAt, "id-token-hint", "access", "refresh", expiresAt, "csrf", revokedAt);
    }

    private GatewaySession activeSession(String token, Instant expiresAt) {
        Instant now = Instant.now();
        return session(token, now, expiresAt, now, null);
    }

    @Test
    void shouldStoreAndRetrieveSession() throws Exception {
        GatewaySession session = activeSession("t1", Instant.now().plusSeconds(3600));
        store.put(session);

        String json = objectMapper.writeValueAsString(session);
        when(valueOps.get("gateway:session:t1")).thenReturn(json);

        Optional<GatewaySession> found = store.get("t1");
        assertTrue(found.isPresent());
        assertEquals("t1", found.get().sessionToken());
        assertEquals(USER_ID, found.get().userId());
        assertEquals(SessionStatus.ACTIVE, store.findByToken("t1").status());
    }

    @Test
    void shouldSetNativeTtlBasedOnEffectiveExpirationPlusTombstoneWindow() {
        Instant now = Instant.now();
        store.put(activeSession("t1", now.plusSeconds(3600)));

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("gateway:session:t1"), anyString(), ttl.capture());
        assertTrue(ttl.getValue().toSeconds() >= 3840, "TTL nativo deve cobrir expiração + janela de tombstone");
        assertTrue(ttl.getValue().toSeconds() <= 3900);
    }

    @Test
    void shouldReturnNotFoundForUnknownOrNullToken() {
        when(valueOps.get("gateway:session:unknown")).thenReturn(null);

        assertTrue(store.get("unknown").isEmpty());
        assertTrue(store.get(null).isEmpty());
        assertEquals(SessionStatus.NOT_FOUND, store.findByToken("unknown").status());
        assertEquals(SessionStatus.NOT_FOUND, store.findByToken(null).status());
    }

    @Test
    void shouldRemoveCorruptedValueAndReturnNotFound() {
        when(valueOps.get("gateway:session:bad")).thenReturn("{corrupted");

        assertEquals(SessionStatus.NOT_FOUND, store.findByToken("bad").status());
        verify(redis).delete("gateway:session:bad");
    }

    @Test
    void shouldExpireByAbsoluteTtlAndRemove() throws Exception {
        GatewaySession session = activeSession("t2", Instant.now().minusSeconds(1));
        store.put(session);
        when(valueOps.get("gateway:session:t2"))
                .thenReturn(objectMapper.writeValueAsString(session));

        assertEquals(SessionStatus.EXPIRED, store.findByToken("t2").status());
        assertTrue(store.get("t2").isEmpty());
        verify(redis, atLeastOnce()).delete("gateway:session:t2");
    }

    @Test
    void shouldRevokeWithTombstone() throws Exception {
        GatewaySession session = activeSession("t5", Instant.now().plusSeconds(3600));
        store.put(session);
        when(valueOps.get("gateway:session:t5"))
                .thenReturn(objectMapper.writeValueAsString(session));

        store.revoke("t5");
        ArgumentCaptor<String> revokedJson = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps, times(2)).set(eq("gateway:session:t5"), revokedJson.capture(), ttl.capture());

        GatewaySession revoked = objectMapper.readValue(revokedJson.getValue(), GatewaySession.class);
        assertTrue(revoked.isRevoked());
        assertTrue(ttl.getValue().toSeconds() <= 300, "tombstone deve ter TTL da retenção curta");

        when(valueOps.get("gateway:session:t5")).thenReturn(revokedJson.getValue());
        assertEquals(SessionStatus.REVOKED, store.findByToken("t5").status());
        assertTrue(store.get("t5").isEmpty(), "sessão revogada não deve ser retornada como ativa");
    }

    @Test
    void shouldBeIdempotentWhenRevokingTwice() throws Exception {
        GatewaySession session = activeSession("t6", Instant.now().plusSeconds(3600));
        GatewaySession revoked = session.withRevokedAt(Instant.now());
        when(valueOps.get("gateway:session:t6"))
                .thenReturn(objectMapper.writeValueAsString(revoked));

        store.revoke("t6");
        store.revoke("t6");
        verify(valueOps, never()).set(eq("gateway:session:t6"), anyString(), any(Duration.class));
    }

    @Test
    void shouldTouchLastAccessedOnActiveLookup() throws Exception {
        Instant now = Instant.now();
        GatewaySession session = session("t4", now, now.plusSeconds(3600), now.minusSeconds(60), null);
        store.put(session);
        when(valueOps.get("gateway:session:t4"))
                .thenReturn(objectMapper.writeValueAsString(session));

        SessionStatus status = store.findByToken("t4").status();
        assertEquals(SessionStatus.ACTIVE, status);

        ArgumentCaptor<String> touchedJson = ArgumentCaptor.forClass(String.class);
        verify(valueOps, times(2)).set(eq("gateway:session:t4"), touchedJson.capture(), any(Duration.class));
        GatewaySession touched = objectMapper.readValue(touchedJson.getValue(), GatewaySession.class);
        assertTrue(touched.lastAccessedAt().isAfter(now.minusSeconds(5)),
                "lookup ativo deve renovar lastAccessedAt");
    }

    @Test
    void shouldAcquireAndReleaseDistributedLock() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        GatewaySessionLock lock = store.lockFor("t1");

        verify(valueOps).setIfAbsent(eq("gateway:refresh-lock:t1"), anyString(), any(Duration.class));

        lock.close();
        verify(redis).execute(any(DefaultRedisScript.class), eq(List.of("gateway:refresh-lock:t1")), anyString());
    }

    @Test
    void shouldTimeoutWhenLockIsHeld() {
        properties.setSessionLockAcquireTimeout(Duration.ofMillis(100));
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> store.lockFor("t1"));
        assertEquals("SESSION_LOCK_TIMEOUT", ex.getCode());
        assertEquals(503, ex.getStatus());
    }

    @Test
    void shouldReportSizeFromSessionKeys() {
        when(redis.keys("gateway:session:*")).thenReturn(Set.of("gateway:session:a", "gateway:session:b"));
        assertEquals(2, store.size());
    }

    @Test
    void shouldPurgeBeNoOpRelyingOnNativeTtl() {
        store.purgeExpired();
        verify(valueOps, never()).set(anyString(), anyString());
    }

    @Test
    void shouldTranslateRedisFailureToDomainUnavailableError() {
        when(valueOps.get("gateway:session:tX"))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Connection refused"));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class, () -> store.findByToken("tX"));
        assertEquals("REDIS_UNAVAILABLE", ex.getCode());
        assertEquals(503, ex.getStatus());
        assertFalse(ex.getMessage().contains("refused"),
                "não deve vazar detalhes da exceção de infraestrutura");
    }

    @Test
    void shouldPersistSwitchToActiveCompanyAcrossRedisRoundTrip() throws Exception {
        UUID companyB = UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444");
        GatewaySession original = activeSession("sw2", Instant.now().plusSeconds(3600));
        store.put(original);

        GatewaySession switched = original.withCompanyId(companyB);
        when(valueOps.get("gateway:session:sw2")).thenReturn(objectMapper.writeValueAsString(switched));
        assertEquals(companyB, store.get("sw2").get().companyId(), "troca A->B persistida via Redis");

        when(valueOps.get("gateway:session:sw2")).thenReturn(
                objectMapper.writeValueAsString(switched.withCompanyId(COMPANY_ID)));
        assertEquals(COMPANY_ID, store.get("sw2").get().companyId(), "troca B->A restaura a empresa A");
    }

    @Test
    void shouldTranslateRedisFailureOnWriteToDomainUnavailableError() {
        org.mockito.Mockito.doThrow(new org.springframework.dao.QueryTimeoutException("connection timed out after 2000 ms"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> store.put(activeSession("tY", Instant.now().plusSeconds(3600))));
        assertEquals("REDIS_UNAVAILABLE", ex.getCode());
        assertEquals(503, ex.getStatus());
    }
}
