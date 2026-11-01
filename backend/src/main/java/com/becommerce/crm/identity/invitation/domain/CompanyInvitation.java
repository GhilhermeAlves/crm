package com.becommerce.crm.identity.invitation.domain;

import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite de criação de empresa (V123). Só o CRM_ADMIN cria; ele fixa
 * segmento, módulos, plano e limite de usuários. No aceite, {@code companyId}
 * passa a apontar a empresa criada pelo convidado.
 *
 * <p>O token nunca é persistido — só {@code tokenHash} (SHA-256).
 */
public class CompanyInvitation {

    public static final long DEFAULT_TTL_DAYS = Invitation.DEFAULT_TTL_DAYS;

    private UUID id;
    private UUID inviterCompanyId;
    private String email;
    private String inviteeName;
    private String segment;
    private String enabledModules;
    private String plan;
    private int maxUsers;
    private String tokenHash;
    private UUID invitedBy;
    private InvitationStatus status;
    private UUID companyId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CompanyInvitation() {
    }

    public static CompanyInvitation create(UUID inviterCompanyId, String email, String inviteeName,
                                           String segment, String enabledModules, String plan, int maxUsers,
                                           String tokenHash, UUID invitedBy) {
        LocalDateTime now = LocalDateTime.now();
        CompanyInvitation i = new CompanyInvitation();
        i.id = UUID.randomUUID();
        i.inviterCompanyId = inviterCompanyId;
        i.email = email;
        i.inviteeName = inviteeName;
        i.segment = segment;
        i.enabledModules = enabledModules;
        i.plan = plan;
        i.maxUsers = maxUsers;
        i.tokenHash = tokenHash;
        i.invitedBy = invitedBy;
        i.status = InvitationStatus.PENDING;
        i.expiresAt = now.plusDays(DEFAULT_TTL_DAYS);
        i.createdAt = now;
        i.updatedAt = now;
        return i;
    }

    /** Status efetivo: um PENDING com validade vencida conta como EXPIRED. */
    public InvitationStatus effectiveStatus() {
        return status == InvitationStatus.PENDING && isExpired() ? InvitationStatus.EXPIRED : status;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Troca o token e renova a validade; só para convites ainda não usados. */
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

    /** Marca como aceito e guarda a empresa criada pelo convidado. */
    public void accept(UUID createdCompanyId) {
        if (status != InvitationStatus.PENDING || isExpired()) {
            throw new InvitationNoLongerValidException(effectiveStatus());
        }
        status = InvitationStatus.ACCEPTED;
        companyId = createdCompanyId;
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
    public UUID getInviterCompanyId() { return inviterCompanyId; }
    public void setInviterCompanyId(UUID inviterCompanyId) { this.inviterCompanyId = inviterCompanyId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInviteeName() { return inviteeName; }
    public void setInviteeName(String inviteeName) { this.inviteeName = inviteeName; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public String getEnabledModules() { return enabledModules; }
    public void setEnabledModules(String enabledModules) { this.enabledModules = enabledModules; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public UUID getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
