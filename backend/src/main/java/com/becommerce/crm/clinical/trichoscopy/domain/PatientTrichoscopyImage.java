package com.becommerce.crm.clinical.trichoscopy.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** Foto tirada com o tricoscópio, guardada no storage e vinculada ao paciente. */
public class PatientTrichoscopyImage {

    private UUID id;
    private UUID companyId;
    private UUID contactId;
    private UUID storageObjectId;
    private UUID takenBy;
    private LocalDateTime takenAt;
    private String notes;
    private LocalDateTime createdAt;

    public static PatientTrichoscopyImage create(UUID companyId, UUID contactId, UUID storageObjectId,
                                                 UUID takenBy, String notes) {
        LocalDateTime now = LocalDateTime.now();
        PatientTrichoscopyImage image = new PatientTrichoscopyImage();
        image.id = UUID.randomUUID();
        image.companyId = companyId;
        image.contactId = contactId;
        image.storageObjectId = storageObjectId;
        image.takenBy = takenBy;
        image.takenAt = now;
        image.notes = notes;
        image.createdAt = now;
        return image;
    }

    public static PatientTrichoscopyImage reconstitute(UUID id, UUID companyId, UUID contactId,
                                                       UUID storageObjectId, UUID takenBy, LocalDateTime takenAt,
                                                       String notes, LocalDateTime createdAt) {
        PatientTrichoscopyImage image = new PatientTrichoscopyImage();
        image.id = id;
        image.companyId = companyId;
        image.contactId = contactId;
        image.storageObjectId = storageObjectId;
        image.takenBy = takenBy;
        image.takenAt = takenAt;
        image.notes = notes;
        image.createdAt = createdAt;
        return image;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getStorageObjectId() { return storageObjectId; }
    public UUID getTakenBy() { return takenBy; }
    public LocalDateTime getTakenAt() { return takenAt; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
