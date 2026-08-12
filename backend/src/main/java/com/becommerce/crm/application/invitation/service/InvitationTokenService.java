package com.becommerce.crm.application.invitation.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração/hash de token de convite (Sprint 8.5).
 *
 * <p>O token é aleatório (32 bytes, Base64URL), imprevisível e de uso único.
 * Apenas o SHA-256 do token (hex, 64 chars) é persistido em
 * {@code invitations.token_hash}; o token em claro existe somente no envio do
 * e-mail.
 */
public final class InvitationTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private InvitationTokenService() {
    }

    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}