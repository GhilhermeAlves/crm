package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anamnesis_questions")
public class AnamnesisQuestionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "is_required")
    private boolean required;

    @Column(name = "highlight")
    private boolean highlight;

    @Column(name = "allow_complement")
    private boolean allowComplement;

    @Column(name = "complement_label")
    private String complementLabel;

    @Column(name = "complement_trigger")
    private String complementTrigger;

    @Column(name = "complement_option_value")
    private String complementOptionValue;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isHighlight() { return highlight; }
    public void setHighlight(boolean highlight) { this.highlight = highlight; }
    public boolean isAllowComplement() { return allowComplement; }
    public void setAllowComplement(boolean allowComplement) { this.allowComplement = allowComplement; }
    public String getComplementLabel() { return complementLabel; }
    public void setComplementLabel(String complementLabel) { this.complementLabel = complementLabel; }
    public String getComplementTrigger() { return complementTrigger; }
    public void setComplementTrigger(String complementTrigger) { this.complementTrigger = complementTrigger; }
    public String getComplementOptionValue() { return complementOptionValue; }
    public void setComplementOptionValue(String complementOptionValue) {
        this.complementOptionValue = complementOptionValue;
    }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
