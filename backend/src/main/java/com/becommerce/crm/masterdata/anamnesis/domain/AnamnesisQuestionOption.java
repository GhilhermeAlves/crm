package com.becommerce.crm.masterdata.anamnesis.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Opção de resposta de uma pergunta de seleção (tabela {@code anamnesis_question_options}). */
public class AnamnesisQuestionOption {

    private final UUID id;
    private final UUID questionId;
    private final UUID companyId;
    private final String label;
    private final String value;
    private final int sortOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AnamnesisQuestionOption(UUID id, UUID questionId, UUID companyId, String label, String value,
                                    int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.questionId = Objects.requireNonNull(questionId, "questionId");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Rótulo da opção é obrigatório");
        }
        this.label = label.trim();
        this.value = value == null || value.isBlank() ? label.trim() : value.trim();
    }

    public static AnamnesisQuestionOption create(UUID id, UUID questionId, UUID companyId, String label,
                                                 String value, int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        return new AnamnesisQuestionOption(id, questionId, companyId, label, value, sortOrder, now, now);
    }

    public static AnamnesisQuestionOption reconstitute(UUID id, UUID questionId, UUID companyId, String label,
                                                       String value, int sortOrder,
                                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AnamnesisQuestionOption(id, questionId, companyId, label, value, sortOrder, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public UUID getCompanyId() { return companyId; }
    public String getLabel() { return label; }
    public String getValue() { return value; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
