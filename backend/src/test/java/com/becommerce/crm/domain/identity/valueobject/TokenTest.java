package com.becommerce.crm.domain.identity.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TokenTest {

    @Test
    void shouldCreateTokenWithAllValues() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Token token = new Token("access-token", userId, companyId, "family-1");

        assertEquals("access-token", token.value());
        assertEquals(userId, token.userId());
        assertEquals(companyId, token.companyId());
        assertEquals("family-1", token.family());
    }

    @Test
    void shouldRejectMissingRequiredValues() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new Token(null, userId, companyId, "family"));
        assertThrows(IllegalArgumentException.class, () -> new Token("   ", userId, companyId, "family"));
        assertThrows(IllegalArgumentException.class, () -> new Token("token", null, companyId, "family"));
        assertThrows(IllegalArgumentException.class, () -> new Token("token", userId, null, "family"));
    }
}
