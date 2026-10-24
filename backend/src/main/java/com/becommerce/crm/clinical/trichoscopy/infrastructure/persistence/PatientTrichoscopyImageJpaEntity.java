package com.becommerce.crm.clinical.trichoscopy.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_trichoscopy_images")
public class PatientTrichoscopyImageJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "storage_object_id", nullable = false)
    private UUID storageObjectId;

    @Column(name = "taken_by")
    private UUID takenBy;

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public UUID getStorageObjectId() { return storageObjectId; }
    public void setStorageObjectId(UUID storageObjectId) { this.storageObjectId = storageObjectId; }
    public UUID getTakenBy() { return takenBy; }
    public void setTakenBy(UUID takenBy) { this.takenBy = takenBy; }
    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
