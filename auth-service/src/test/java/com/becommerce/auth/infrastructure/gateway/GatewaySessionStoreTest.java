package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySessionStoreTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final GatewaySessionStore store = new GatewaySessionStore();

    private GatewaySession session(String token, Instant expiresAt) {
        return new GatewaySession(token, USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "A", Instant.now(), expiresAt);
    }

    @Test
    void shouldStoreAndRetrieveSession() {
        GatewaySession session = session("t1", Instant.now().plusSeconds(3600));
        store.put(session);

        Optional<GatewaySession> found = store.get("t1");
        assertTrue(found.isPresent());
        assertEquals("t1", found.get().sessionToken());
        assertEquals(USER_ID, found.get().userId());
    }

    @Test
    void shouldReturnEmptyForUnknownOrNullToken() {
        assertTrue(store.get("unknown").isEmpty());
        assertTrue(store.get(null).isEmpty());
    }

    @Test
    void shouldExpireSessionAndRemoveIt() {
        store.put(session("t2", Instant.now().minusSeconds(1)));
        assertTrue(store.get("t2").isEmpty());
        assertEquals(0, store.size(), "expired session must be removed on access");
    }

    @Test
    void shouldRevokeSession() {
        store.put(session("t3", Instant.now().plusSeconds(3600)));
        store.revoke("t3");
        assertTrue(store.get("t3").isEmpty());
    }

    @Test
    void shouldPurgeExpiredSessions() {
        store.put(session("expired", Instant.now().minusSeconds(1)));
        store.put(session("fresh", Instant.now().plusSeconds(3600)));
        store.purgeExpired();
        assertEquals(1, store.size());
        assertTrue(store.get("fresh").isPresent());
    }
}
