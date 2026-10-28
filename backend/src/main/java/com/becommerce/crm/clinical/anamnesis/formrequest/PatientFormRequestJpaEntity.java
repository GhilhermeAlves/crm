package com.becommerce.crm.clinical.anamnesis.formrequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** Pedido de formulário respondido pelo paciente (V115). */
@Entity
@Table(name = "patient_form_requests")
public class PatientFormRequestJpaEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ANSWERED = "ANSWERED";

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "anamnesis_id")
    private UUID anamnesisId;

    @Column(name = "token")
    private String tokenHash;

    @Column(name = "mode")
    private String mode;

    @Column(name = "status")
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "signature_storage_id")
    private UUID signatureStorageId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected PatientFormRequestJpaEntity() {
    }

    public PatientFormRequestJpaEntity(UUID companyId, UUID contactId, UUID anamnesisId, String tokenHash,
                                       String mode, LocalDateTime expiresAt, UUID createdBy,
                                       LocalDateTime now) {
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.contactId = contactId;
        this.anamnesisId = anamnesisId;
        this.tokenHash = tokenHash;
        this.mode = mode;
        this.status = STATUS_PENDING;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void markAnswered(LocalDateTime now, UUID signatureId) {
        this.status = STATUS_ANSWERED;
        this.answeredAt = now;
        this.signatureStorageId = signatureId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public UUID getAnamnesisId() {
        return anamnesisId;
    }

    public String getMode() {
        return mode;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public UUID getSignatureStorageId() {
        return signatureStorageId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
