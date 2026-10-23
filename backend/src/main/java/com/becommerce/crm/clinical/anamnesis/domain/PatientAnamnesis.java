package com.becommerce.crm.clinical.anamnesis.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Anamnese preenchida de um paciente (ficha do paciente, Fase 2). Guarda uma
 * cópia (snapshot) do modelo no momento do preenchimento; editar o modelo
 * depois não altera anamneses antigas. {@code DRAFT} é editável pelo autor ou
 * administradores; {@code COMPLETED} é imutável. Tabela
 * {@code patient_anamneses} (V105), com RLS FORCE por empresa.
 *
 * <p>Snapshot e respostas trafegam como JSON (strings) e são convertidos na
 * camada de aplicação; {@code alerts} já é a lista pronta para exibição.
 */
public class PatientAnamnesis {

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private final UUID modelId;
    private final String modelName;
    private final int modelVersion;
    private final String snapshot;
    private String answers;
    private List<String> alerts;
    private PatientAnamnesisStatus status;
    private final UUID createdBy;
    private UUID completedBy;
    private LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PatientAnamnesis(UUID id, UUID companyId, UUID contactId, UUID modelId, String modelName,
                             int modelVersion, String snapshot, String answers, List<String> alerts,
                             PatientAnamnesisStatus status, UUID createdBy, UUID completedBy,
                             LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.modelId = modelId;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.snapshot = snapshot;
        this.answers = answers;
        this.alerts = alerts;
        this.status = status;
        this.createdBy = createdBy;
        this.completedBy = completedBy;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PatientAnamnesis create(UUID companyId, UUID contactId, UUID modelId, String modelName,
                                          int modelVersion, String snapshot, UUID createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return new PatientAnamnesis(UUID.randomUUID(), companyId, contactId, modelId, modelName,
                modelVersion, snapshot, "{}", List.of(), PatientAnamnesisStatus.DRAFT, createdBy,
                null, null, now, now);
    }

    public static PatientAnamnesis reconstitute(UUID id, UUID companyId, UUID contactId, UUID modelId,
                                                String modelName, int modelVersion, String snapshot,
                                                String answers, List<String> alerts,
                                                PatientAnamnesisStatus status, UUID createdBy,
                                                UUID completedBy, LocalDateTime completedAt,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PatientAnamnesis(id, companyId, contactId, modelId, modelName, modelVersion, snapshot,
                answers, alerts == null ? List.of() : alerts, status, createdBy, completedBy, completedAt,
                createdAt, updatedAt);
    }

    public void saveDraft(String answersJson) {
        assertDraft();
        this.answers = answersJson;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String answersJson, List<String> alerts, UUID completedBy) {
        assertDraft();
        this.answers = answersJson;
        this.alerts = alerts == null ? List.of() : alerts;
        this.status = PatientAnamnesisStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = this.completedAt;
    }

    public boolean isDraft() {
        return status == PatientAnamnesisStatus.DRAFT;
    }

    public boolean isAuthoredBy(UUID userId) {
        return createdBy != null && createdBy.equals(userId);
    }

    private void assertDraft() {
        if (!isDraft()) {
            throw new ContactValidationException(
                    "Anamnese concluída não pode ser alterada. Inicie uma nova.");
        }
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public int getModelVersion() { return modelVersion; }
    public String getSnapshot() { return snapshot; }
    public String getAnswers() { return answers; }
    public List<String> getAlerts() { return alerts; }
    public PatientAnamnesisStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getCompletedBy() { return completedBy; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
