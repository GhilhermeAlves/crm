package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anamnesis_sections")
public class AnamnesisSectionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "title")
    private String title;

    @Column(name = "is_professional")
    private boolean professional;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isProfessional() { return professional; }
    public void setProfessional(boolean professional) { this.professional = professional; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
