package com.becommerce.crm.masterdata.anamnesis.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Seção de um modelo de anamnese (tabela {@code anamnesis_sections}). */
public class AnamnesisSection {

    private final UUID id;
    private final UUID modelId;
    private final UUID companyId;
    private String title;
    private boolean professional;
    private int sortOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AnamnesisSection(UUID id, UUID modelId, UUID companyId, String title, boolean professional,
                             int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.professional = professional;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        applyTitle(title);
    }

    public static AnamnesisSection create(UUID id, UUID modelId, UUID companyId, String title,
                                          boolean professional, int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        return new AnamnesisSection(id, modelId, companyId, title, professional, sortOrder, now, now);
    }

    public static AnamnesisSection reconstitute(UUID id, UUID modelId, UUID companyId, String title,
                                                boolean professional, int sortOrder,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AnamnesisSection(id, modelId, companyId, title, professional, sortOrder, createdAt, updatedAt);
    }

    private void applyTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Título da seção é obrigatório");
        }
        this.title = title.trim();
    }

    public UUID getId() { return id; }
    public UUID getModelId() { return modelId; }
    public UUID getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public boolean isProfessional() { return professional; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
