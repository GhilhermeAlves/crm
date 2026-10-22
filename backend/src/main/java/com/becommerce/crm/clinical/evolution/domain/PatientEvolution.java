package com.becommerce.crm.clinical.evolution.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evolução clínica do paciente (ficha do paciente, Fase 2). Nasce como
 * {@code DRAFT}, editável pelo autor ou por administradores; ao finalizar vira
 * {@code FINALIZED} e não aceita mais alterações (o trigger
 * {@code trg_patient_evolutions_immutable} da V105 é a segunda barreira).
 */
public class PatientEvolution {

    static final int MAX_BODY_LENGTH = 20000;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private UUID appointmentId;
    private String body;
    private PatientEvolutionStatus status;
    private final UUID createdBy;
    private UUID finalizedBy;
    private LocalDateTime finalizedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PatientEvolution(UUID id, UUID companyId, UUID contactId, UUID appointmentId, String body,
                             PatientEvolutionStatus status, UUID createdBy, UUID finalizedBy,
                             LocalDateTime finalizedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.appointmentId = appointmentId;
        this.body = body;
        this.status = status;
        this.createdBy = createdBy;
        this.finalizedBy = finalizedBy;
        this.finalizedAt = finalizedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PatientEvolution create(UUID companyId, UUID contactId, String body, UUID appointmentId,
                                          UUID createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return new PatientEvolution(UUID.randomUUID(), companyId, contactId, appointmentId,
                normalizeBody(body), PatientEvolutionStatus.DRAFT, createdBy, null, null, now, now);
    }

    public static PatientEvolution reconstitute(UUID id, UUID companyId, UUID contactId, UUID appointmentId,
                                                String body, PatientEvolutionStatus status, UUID createdBy,
                                                UUID finalizedBy, LocalDateTime finalizedAt,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PatientEvolution(id, companyId, contactId, appointmentId, body, status, createdBy,
                finalizedBy, finalizedAt, createdAt, updatedAt);
    }

    public void edit(String body, UUID appointmentId) {
        assertDraft();
        this.body = normalizeBody(body);
        this.appointmentId = appointmentId;
        this.updatedAt = LocalDateTime.now();
    }

    public void finalizeBy(UUID userId) {
        assertDraft();
        this.status = PatientEvolutionStatus.FINALIZED;
        this.finalizedBy = userId;
        this.finalizedAt = LocalDateTime.now();
        this.updatedAt = this.finalizedAt;
    }

    public boolean isDraft() {
        return status == PatientEvolutionStatus.DRAFT;
    }

    public boolean isAuthoredBy(UUID userId) {
        return createdBy != null && createdBy.equals(userId);
    }

    private void assertDraft() {
        if (!isDraft()) {
            throw new ContactValidationException("Evolução finalizada não pode ser alterada.");
        }
    }

    private static String normalizeBody(String body) {
        if (body == null || body.isBlank()) {
            throw new ContactValidationException("O texto da evolução é obrigatório.");
        }
        String trimmed = body.trim();
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new ContactValidationException(
                    "O texto da evolução deve ter no máximo " + MAX_BODY_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getAppointmentId() { return appointmentId; }
    public String getBody() { return body; }
    public PatientEvolutionStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getFinalizedBy() { return finalizedBy; }
    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
