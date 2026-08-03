package com.becommerce.auth.infrastructure.gateway;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PkceGeneratorTest {

    private static final Pattern UNRESERVED = Pattern.compile("^[A-Za-z0-9\\-._~]+$");

    private final PkceGenerator generator = new PkceGenerator(new SecureTokenGenerator());

    @Test
    void shouldGenerateValidCodeVerifier() {
        String verifier = generator.codeVerifier();
        assertTrue(verifier.length() >= 43 && verifier.length() <= 128,
                "verifier length must be 43-128 (RFC 7636)");
        assertTrue(UNRESERVED.matcher(verifier).matches(), "verifier must use unreserved chars");
    }

    @Test
    void shouldGenerateDistinctVerifiers() {
        Set<String> verifiers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            verifiers.add(generator.codeVerifier());
        }
        assertEquals(100, verifiers.size());
    }

    @Test
    void shouldComputeDeterministicS256Challenge() throws Exception {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertEquals(expected, generator.codeChallengeS256(verifier));
    }

    @Test
    void shouldProduceDifferentChallengeForDifferentVerifier() {
        assertNotEquals(generator.codeChallengeS256(generator.codeVerifier()),
                generator.codeChallengeS256(generator.codeVerifier()));
    }

    @Test
    void shouldRejectVerifierOutsideAllowedLength() {
        assertThrows(IllegalArgumentException.class, () -> generator.codeChallengeS256("too-short"));
    }
}
