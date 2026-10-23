package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patient_procedures")
public class PatientProcedureJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    @Column(name = "name_snapshot")
    private String nameSnapshot;

    @Column(name = "state")
    private String state;

    @Column(name = "dentition")
    private String dentition;

    @Column(name = "price_cents")
    private long priceCents;

    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "performed_at")
    private LocalDate performedAt;

    @Column(name = "financial_transaction_id")
    private UUID financialTransactionId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_procedure_teeth", joinColumns = @JoinColumn(name = "procedure_id"))
    private List<PatientProcedureToothJpaEntity> teeth = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(UUID catalogItemId) { this.catalogItemId = catalogItemId; }
    public String getNameSnapshot() { return nameSnapshot; }
    public void setNameSnapshot(String nameSnapshot) { this.nameSnapshot = nameSnapshot; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getDentition() { return dentition; }
    public void setDentition(String dentition) { this.dentition = dentition; }
    public long getPriceCents() { return priceCents; }
    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }
    public UUID getProfessionalId() { return professionalId; }
    public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDate performedAt) { this.performedAt = performedAt; }
    public UUID getFinancialTransactionId() { return financialTransactionId; }
    public void setFinancialTransactionId(UUID financialTransactionId) { this.financialTransactionId = financialTransactionId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<PatientProcedureToothJpaEntity> getTeeth() { return teeth; }
    public void setTeeth(List<PatientProcedureToothJpaEntity> teeth) { this.teeth = teeth; }
}
