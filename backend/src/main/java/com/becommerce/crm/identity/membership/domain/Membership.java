package com.becommerce.crm.identity.membership.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Relação de adesão usuário ↔ empresa (Sprint 8.2). Fonte de verdade do vínculo
 * 1:N; {@code users.company_id} permanece como "empresa ativa" denormalizada.
 *
 * <p>Papel ({@code role}) armazenado como nome (ex.: {@code ADMIN}) e mantido em
 * sincronia com {@code user_roles} pela camada de aplicação.
 */
public class Membership {

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String OWNER_ROLE = "OWNER";
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private UUID id;
    private UUID userId;
    private UUID companyId;
    private String role;
    private String specialty;
    private MembershipStatus status;
    private UUID invitedBy;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Membership() {
    }

    public static Membership activate(UUID userId, UUID companyId, String role) {
        return activate(userId, companyId, role, null);
    }

    public static Membership activate(UUID userId, UUID companyId, String role, String specialty) {
        Membership membership = new Membership();
        membership.userId = userId;
        membership.companyId = companyId;
        membership.role = role;
        membership.specialty = specialty;
        membership.status = MembershipStatus.ACTIVE;
        membership.joinedAt = LocalDateTime.now();
        membership.createdAt = membership.joinedAt;
        membership.updatedAt = membership.joinedAt;
        return membership;
    }

    public static Membership invite(UUID userId, UUID companyId, String role, UUID invitedBy) {
        return invite(userId, companyId, role, invitedBy, null);
    }

    public static Membership invite(UUID userId, UUID companyId, String role, UUID invitedBy, String specialty) {
        Membership membership = activate(userId, companyId, role, specialty);
        membership.status = MembershipStatus.PENDING;
        membership.invitedBy = invitedBy;
        return membership;
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    public boolean isAdminRole() {
        return isAdminLevelRole(role);
    }

    /** True se o nome do papel é de nível administrador (ex.: ADMIN, OWNER, SUPER_ADMIN). */
    public static boolean isAdminLevelRole(String role) {
        return ADMIN_ROLE.equals(role) || OWNER_ROLE.equals(role) || SUPER_ADMIN_ROLE.equals(role);
    }

    public void changeRole(String newRole) {
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
    }

    public void remove() {
        this.status = MembershipStatus.REMOVED;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
