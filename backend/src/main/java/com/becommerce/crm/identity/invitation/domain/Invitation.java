package com.becommerce.crm.identity.invitation.domain;

import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite de empresa (Sprint 8.5).
 *
 * <p>O token em si nunca é persistido — apenas {@code tokenHash} (SHA-256). A
 * expiração e as transições de estado são validadas no momento do aceite (nunca
 * dependem de job): usar {@link #isExpired()} + {@link #accept()}.
 */
public class Invitation {

    public static final long DEFAULT_TTL_DAYS = 7;

    private UUID id;
    private UUID companyId;
    private String email;
    private String inviteeName;
    private String role;
    private String tokenHash;
    private UUID invitedBy;
    private InvitationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Invitation() {
    }

    public Invitation(UUID id, UUID companyId, String email, String inviteeName, String role, String tokenHash,
                      UUID invitedBy, InvitationStatus status, LocalDateTime expiresAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.email = email;
        this.inviteeName = inviteeName;
        this.role = role;
        this.tokenHash = tokenHash;
        this.invitedBy = invitedBy;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Invitation create(UUID companyId, String email, String role,
                                    String tokenHash, UUID invitedBy) {
        return create(companyId, email, null, role, tokenHash, invitedBy);
    }

    public static Invitation create(UUID companyId, String email, String inviteeName, String role,
                                    String tokenHash, UUID invitedBy) {
        LocalDateTime now = LocalDateTime.now();
        return new Invitation(UUID.randomUUID(), companyId, email, inviteeName, role, tokenHash,
                invitedBy, InvitationStatus.PENDING, now.plusDays(DEFAULT_TTL_DAYS),
                now, now);
    }

    /** Status efetivo: um PENDING com validade vencida conta como EXPIRED. */
    public InvitationStatus effectiveStatus() {
        return status == InvitationStatus.PENDING && isExpired() ? InvitationStatus.EXPIRED : status;
    }

    /**
     * Troca o token (o anterior deixa de funcionar) e renova a validade. Só para
     * convites ainda não usados: PENDING ou EXPIRED.
     */
    public void renew(String newTokenHash) {
        if (status != InvitationStatus.PENDING && status != InvitationStatus.EXPIRED) {
            throw new InvitationNoLongerValidException(status);
        }
        LocalDateTime now = LocalDateTime.now();
        tokenHash = newTokenHash;
        status = InvitationStatus.PENDING;
        expiresAt = now.plusDays(DEFAULT_TTL_DAYS);
        updatedAt = now;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void accept() {
        if (status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Convite não está pendente: " + status);
        }
        if (isExpired()) {
            status = InvitationStatus.EXPIRED;
            throw new IllegalStateException("Convite expirado.");
        }
        status = InvitationStatus.ACCEPTED;
        updatedAt = LocalDateTime.now();
    }

    public void revoke() {
        if (status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Somente convites pendentes podem ser revogados.");
        }
        status = InvitationStatus.REVOKED;
        updatedAt = LocalDateTime.now();
    }

    public void markExpired() {
        if (status == InvitationStatus.PENDING && isExpired()) {
            status = InvitationStatus.EXPIRED;
            updatedAt = LocalDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getEmail() { return email; }
    public String getInviteeName() { return inviteeName; }
    public void setInviteeName(String inviteeName) { this.inviteeName = inviteeName; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public UUID getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}