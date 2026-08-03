package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.becommerce.auth.domain.gateway.SessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySessionStoreTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final OidcGatewayProperties properties = new OidcGatewayProperties();
    private final GatewaySessionStore store = new GatewaySessionStore(properties);

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
    void shouldStoreAndRetrieveSession() {
        GatewaySession session = activeSession("t1", Instant.now().plusSeconds(3600));
        store.put(session);

        Optional<GatewaySession> found = store.get("t1");
        assertTrue(found.isPresent());
        assertEquals("t1", found.get().sessionToken());
        assertEquals(USER_ID, found.get().userId());
        assertEquals(SessionStatus.ACTIVE, store.findByToken("t1").status());
    }

    @Test
    void shouldReturnEmptyForUnknownOrNullToken() {
        assertTrue(store.get("unknown").isEmpty());
        assertTrue(store.get(null).isEmpty());
        assertEquals(SessionStatus.NOT_FOUND, store.findByToken("unknown").status());
        assertEquals(SessionStatus.NOT_FOUND, store.findByToken(null).status());
    }

    @Test
    void shouldReturnNotFoundForUnknownToken() {
        SessionLookup lookup = store.findByToken("ghost");
        assertEquals(SessionStatus.NOT_FOUND, lookup.status());
        assertNull(lookup.session());
    }

    @Test
    void shouldExpireByAbsoluteTtlAndRemove() {
        store.put(activeSession("t2", Instant.now().minusSeconds(1)));
        assertEquals(SessionStatus.EXPIRED, store.findByToken("t2").status());
        assertTrue(store.get("t2").isEmpty());
        assertEquals(0, store.size(), "expired session must be removed on access");
    }

    @Test
    void shouldExpireByIdleTimeout() {
        properties.setSessionIdleTimeout(Duration.ofMinutes(5));
        Instant now = Instant.now();
        store.put(session("t3", now, now.plusSeconds(3600), now.minusSeconds(301), null));

        assertEquals(SessionStatus.EXPIRED, store.findByToken("t3").status(),
                "sessão ociosa além do idle timeout deve expirar mesmo com TTL absoluto válido");
        assertTrue(store.get("t3").isEmpty());
    }

    @Test
    void shouldTouchLastAccessedOnActiveLookup() {
        properties.setSessionIdleTimeout(Duration.ofMinutes(5));
        Instant now = Instant.now();
        store.put(session("t4", now, now.plusSeconds(3600), now.minusSeconds(60), null));

        SessionLookup first = store.findByToken("t4");
        assertEquals(SessionStatus.ACTIVE, first.status());
        Instant touched = first.session().lastAccessedAt();
        assertTrue(touched.isAfter(now.minusSeconds(5)), "lookup ativo deve renovar lastAccessedAt");
        assertNotEquals(now.minusSeconds(60), touched);
    }

    @Test
    void shouldRevokeSessionWithTombstone() {
        store.put(activeSession("t5", Instant.now().plusSeconds(3600)));
        store.revoke("t5");

        assertTrue(store.get("t5").isEmpty(), "sessão revogada não deve ser retornada como ativa");
        SessionLookup lookup = store.findByToken("t5");
        assertEquals(SessionStatus.REVOKED, lookup.status(), "tombstone deve persistir como REVOKED");
        assertTrue(lookup.session().isRevoked());
    }

    @Test
    void shouldBeIdempotentWhenRevokingTwice() {
        store.put(activeSession("t6", Instant.now().plusSeconds(3600)));
        store.revoke("t6");
        store.revoke("t6");
        assertEquals(SessionStatus.REVOKED, store.findByToken("t6").status());
    }

    @Test
    void shouldKeepTombstoneWithinRetentionAndPurgeAfter() {
        store.put(activeSession("t7", Instant.now().plusSeconds(3600)));
        store.revoke("t7");
        store.purgeExpired();
        assertEquals(SessionStatus.REVOKED, store.findByToken("t7").status(),
                "tombstone recente não deve ser purgado");

        Instant now = Instant.now();
        store.put(session("t8", now, now.plusSeconds(3600), now, now.minus(Duration.ofMinutes(6))));
        store.purgeExpired();
        assertEquals(SessionStatus.NOT_FOUND, store.findByToken("t8").status(),
                "tombstone após a retenção deve ser purgado");
    }

    @Test
    void shouldPurgeExpiredSessionsAndRetainFresh() {
        store.put(activeSession("expired", Instant.now().minusSeconds(1)));
        store.put(activeSession("fresh", Instant.now().plusSeconds(3600)));
        store.purgeExpired();
        assertEquals(1, store.size());
        assertTrue(store.get("fresh").isPresent());
    }
}
