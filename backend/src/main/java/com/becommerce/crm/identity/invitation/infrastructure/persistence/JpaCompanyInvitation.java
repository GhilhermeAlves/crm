package com.becommerce.crm.identity.invitation.infrastructure.persistence;

import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_invitations")
public class JpaCompanyInvitation {

    @Id
    private UUID id;

    @Column(name = "inviter_company_id")
    private UUID inviterCompanyId;

    @Column(name = "email")
    private String email;

    @Column(name = "invitee_name")
    private String inviteeName;

    @Column(name = "segment")
    private String segment;

    @Column(name = "enabled_modules")
    private String enabledModules;

    @Column(name = "plan")
    private String plan;

    @Column(name = "max_users")
    private int maxUsers;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvitationStatus status;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
