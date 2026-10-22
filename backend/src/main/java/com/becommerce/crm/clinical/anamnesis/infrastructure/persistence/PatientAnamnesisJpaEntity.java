package com.becommerce.crm.clinical.anamnesis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patient_anamneses")
public class PatientAnamnesisJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private int modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "jsonb")
    private String snapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb")
    private String answers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alerts", columnDefinition = "jsonb")
    private List<String> alerts;

    @Column(name = "status")
    private String status;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public int getModelVersion() { return modelVersion; }
    public void setModelVersion(int modelVersion) { this.modelVersion = modelVersion; }
    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public List<String> getAlerts() { return alerts; }
    public void setAlerts(List<String> alerts) { this.alerts = alerts; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getCompletedBy() { return completedBy; }
    public void setCompletedBy(UUID completedBy) { this.completedBy = completedBy; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
