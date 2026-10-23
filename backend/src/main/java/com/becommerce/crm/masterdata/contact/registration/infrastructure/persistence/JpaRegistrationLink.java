package com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_registration_links")
public class JpaRegistrationLink {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    /** SHA-256 hex do token entregue ao paciente. */
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected JpaRegistrationLink() {
    }

    public JpaRegistrationLink(UUID id, UUID companyId, UUID contactId, String token,
                               LocalDateTime expiresAt, LocalDateTime usedAt, UUID createdBy,
                               LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public String getToken() { return token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
