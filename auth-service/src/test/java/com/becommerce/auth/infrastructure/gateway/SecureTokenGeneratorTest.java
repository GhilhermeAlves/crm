package com.becommerce.auth.infrastructure.gateway;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureTokenGeneratorTest {

    private static final Pattern URL_SAFE = Pattern.compile("^[A-Za-z0-9\\-_.]+$");

    private final SecureTokenGenerator generator = new SecureTokenGenerator();

    @Test
    void shouldGenerateTokenWithExpectedLength() {
        assertEquals(43, generator.urlSafe(32).length());
        assertEquals(64, generator.urlSafe(48).length());
    }

    @Test
    void shouldGenerateUrlSafeTokenWithoutPadding() {
        String token = generator.urlSafe(32);
        assertTrue(URL_SAFE.matcher(token).matches());
        assertTrue(!token.contains("="), "no base64 padding allowed");
    }

    @Test
    void shouldGenerateDistinctTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(generator.urlSafe(32));
        }
        assertEquals(1000, tokens.size());
    }

    @Test
    void shouldRejectTooSmallByteLength() {
        assertThrows(IllegalArgumentException.class, () -> generator.urlSafe(8));
    }

    @Test
    void shouldNotBePredictableAcrossCalls() {
        assertNotEquals(generator.urlSafe(32), generator.urlSafe(32));
    }
}
