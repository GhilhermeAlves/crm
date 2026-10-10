package com.becommerce.crm.masterdata.anamnesis.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de anamnese de uma empresa (raiz do agregado, tabela {@code anamnesis_models}).
 * Editável: cada empresa possui sua própria instância, inclusive do modelo padrão
 * odontológico. {@code version} é incrementado a cada edição para preparar o
 * versionamento futuro das respostas (uma edição não deve alterar respostas já dadas).
 */
public class AnamnesisModel {

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String description;
    private boolean active;
    private final boolean isDefault;
    private int version;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AnamnesisModel(UUID id, UUID companyId, String name, String description, boolean active,
                           boolean isDefault, int version, UUID createdBy, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.active = active;
        this.isDefault = isDefault;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        apply(name, description);
        this.updatedAt = updatedAt;
    }

    public static AnamnesisModel create(UUID id, UUID companyId, String name, String description,
                                        UUID createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return new AnamnesisModel(id, companyId, name, description, true, false, 1, createdBy, now, now);
    }

    /** Instância padrão provisionada automaticamente para a empresa. */
    public static AnamnesisModel createDefault(UUID id, UUID companyId, String name, String description,
                                              UUID createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return new AnamnesisModel(id, companyId, name, description, true, true, 1, createdBy, now, now);
    }

    public static AnamnesisModel reconstitute(UUID id, UUID companyId, String name, String description,
                                              boolean active, boolean isDefault, int version, UUID createdBy,
                                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AnamnesisModel(id, companyId, name, description, active, isDefault, version, createdBy,
                createdAt, updatedAt);
    }

    public void update(String name, String description) {
        apply(name, description);
        this.version += 1;
        touch();
    }

    public void activate() {
        this.active = true;
        touch();
    }

    public void deactivate() {
        this.active = false;
        touch();
    }

    private void apply(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome do modelo é obrigatório");
        }
        this.name = name.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public boolean isDefault() { return isDefault; }
    public int getVersion() { return version; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
