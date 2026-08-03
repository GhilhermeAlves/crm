package com.becommerce.auth.infrastructure.gateway;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Geração de PKCE (RFC 7636): {@code code_verifier} aleatório (43–128 chars do
 * conjunto unreserved) e {@code code_challenge} S256 (Base64URL(SHA-256(verifier))).
 */
@Component
public class PkceGenerator {

    private final SecureTokenGenerator tokenGenerator;

    public PkceGenerator(SecureTokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    public String codeVerifier() {
        return tokenGenerator.urlSafe(48);
    }

    public String codeChallengeS256(String verifier) {
        if (verifier == null || verifier.length() < 43 || verifier.length() > 128) {
            throw new IllegalArgumentException("code_verifier inválido (RFC 7636): 43-128 chars");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
