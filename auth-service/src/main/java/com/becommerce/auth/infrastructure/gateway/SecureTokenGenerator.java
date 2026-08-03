package com.becommerce.auth.infrastructure.gateway;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera valores opacos criptograficamente aleatórios (URL-safe, sem padding) para
 * {@code state}, {@code nonce}, {@code codeVerifier} (PKCE) e {@code sessionToken}.
 * Usa {@link SecureRandom} — nunca {@code Random}.
 */
@Component
public class SecureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String urlSafe(int byteLength) {
        if (byteLength < 16) {
            throw new IllegalArgumentException("byteLength deve ser >= 16");
        }
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
