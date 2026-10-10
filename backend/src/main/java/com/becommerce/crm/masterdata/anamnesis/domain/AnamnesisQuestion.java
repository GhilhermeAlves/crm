package com.becommerce.crm.masterdata.anamnesis.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Pergunta de uma seção de anamnese (tabela {@code anamnesis_questions}). */
public class AnamnesisQuestion {

    private final UUID id;
    private final UUID sectionId;
    private final UUID modelId;
    private final UUID companyId;
    private final String text;
    private final AnamnesisQuestionType type;
    private final boolean required;
    private final boolean highlight;
    private final boolean allowComplement;
    private final String complementLabel;
    private final AnamnesisComplementTrigger complementTrigger;
    private final String complementOptionValue;
    private final int sortOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AnamnesisQuestion(UUID id, UUID sectionId, UUID modelId, UUID companyId, String text,
                              AnamnesisQuestionType type, boolean required, boolean highlight,
                              boolean allowComplement, String complementLabel,
                              AnamnesisComplementTrigger complementTrigger, String complementOptionValue,
                              int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.sectionId = Objects.requireNonNull(sectionId, "sectionId");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.required = required;
        this.highlight = highlight;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.type = Objects.requireNonNull(type, "type");
        this.complementTrigger = complementTrigger == null ? AnamnesisComplementTrigger.YES : complementTrigger;
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Texto da pergunta é obrigatório");
        }
        this.text = text.trim();
        this.allowComplement = allowComplement;
        this.complementLabel = blankToNull(complementLabel);
        this.complementOptionValue = blankToNull(complementOptionValue);
    }

    public static AnamnesisQuestion create(UUID id, UUID sectionId, UUID modelId, UUID companyId, String text,
                                           AnamnesisQuestionType type, boolean required, boolean highlight,
                                           boolean allowComplement, String complementLabel,
                                           AnamnesisComplementTrigger complementTrigger,
                                           String complementOptionValue, int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        return new AnamnesisQuestion(id, sectionId, modelId, companyId, text, type, required, highlight,
                allowComplement, complementLabel, complementTrigger, complementOptionValue, sortOrder, now, now);
    }

    public static AnamnesisQuestion reconstitute(UUID id, UUID sectionId, UUID modelId, UUID companyId,
                                                 String text, AnamnesisQuestionType type, boolean required,
                                                 boolean highlight, boolean allowComplement,
                                                 String complementLabel,
                                                 AnamnesisComplementTrigger complementTrigger,
                                                 String complementOptionValue, int sortOrder,
                                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AnamnesisQuestion(id, sectionId, modelId, companyId, text, type, required, highlight,
                allowComplement, complementLabel, complementTrigger, complementOptionValue, sortOrder,
                createdAt, updatedAt);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getSectionId() { return sectionId; }
    public UUID getModelId() { return modelId; }
    public UUID getCompanyId() { return companyId; }
    public String getText() { return text; }
    public AnamnesisQuestionType getType() { return type; }
    public boolean isRequired() { return required; }
    public boolean isHighlight() { return highlight; }
    public boolean isAllowComplement() { return allowComplement; }
    public String getComplementLabel() { return complementLabel; }
    public AnamnesisComplementTrigger getComplementTrigger() { return complementTrigger; }
    public String getComplementOptionValue() { return complementOptionValue; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
