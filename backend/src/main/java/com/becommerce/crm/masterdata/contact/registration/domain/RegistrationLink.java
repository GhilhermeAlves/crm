package com.becommerce.crm.masterdata.contact.registration.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Link público de cadastro do paciente (V108). O token entregue ao paciente
 * (32 bytes {@link SecureRandom}, Base64URL) só existe na URL; persiste-se
 * apenas o SHA-256 hex ({@code patient_registration_links.token}), como o convite.
 */
public class RegistrationLink {

    public static final int VALIDITY_DAYS = 7;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime usedAt;
    private final UUID createdBy;
    private final LocalDateTime createdAt;

    public RegistrationLink(UUID id, UUID companyId, UUID contactId, String tokenHash,
                            LocalDateTime expiresAt, LocalDateTime usedAt, UUID createdBy,
                            LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static RegistrationLink create(UUID companyId, UUID contactId, String tokenHash,
                                          UUID createdBy, LocalDateTime now) {
        return new RegistrationLink(UUID.randomUUID(), companyId, contactId, tokenHash,
                now.plusDays(VALIDITY_DAYS), null, createdBy, now);
    }

    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
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

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
