package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcAuthorizationRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OidcAuthorizationRequestStoreTest {

    private final OidcAuthorizationRequestStore store = new OidcAuthorizationRequestStore();

    private OidcAuthorizationRequest request(String state, Instant expiresAt) {
        return new OidcAuthorizationRequest(state, "nonce", "verifier", "/dashboard", expiresAt);
    }

    @Test
    void shouldConsumeStoredRequestOnce() {
        store.put(request("s1", Instant.now().plusSeconds(600)));

        OidcAuthorizationRequest first = store.consume("s1");
        assertNotNull(first);
        assertEquals("s1", first.getState());
        assertNull(store.consume("s1"), "replay must be rejected (single-use)");
    }

    @Test
    void shouldReturnNullForUnknownState() {
        assertNull(store.consume("unknown"));
        assertNull(store.consume(null));
    }

    @Test
    void shouldRejectExpiredRequestOnConsume() {
        store.put(request("s2", Instant.now().minusSeconds(1)));
        assertNull(store.consume("s2"));
    }

    @Test
    void shouldPurgeOnlyExpiredRequests() {
        store.put(request("expired", Instant.now().minusSeconds(1)));
        store.put(request("fresh", Instant.now().plusSeconds(600)));

        store.purgeExpired();

        assertEquals(1, store.size());
        assertNotNull(store.consume("fresh"));
    }
}
