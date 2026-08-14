package com.becommerce.crm.domain.pipeline;

import com.becommerce.crm.domain.pipeline.exception.PipelineValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pipeline por empresa (Sprint 11). Corresponde à tabela {@code pipelines}
 * (V017), protegida por RLS. Cada empresa pode ter múltiplos pipelines para
 * diferentes produtos/linhas de negócio (P-001). Os estágios são agregados ao
 * pipeline via {@link Stage} (2..15 estágios — P-002/P-003).
 */
public class Pipeline {

    public static final int MIN_STAGES = 2;
    public static final int MAX_STAGES = 15;

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String description;
    private boolean active;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Pipeline(UUID id, UUID companyId, String name, String description, boolean active,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Pipeline create(UUID companyId, String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Pipeline(UUID.randomUUID(), companyId, name, description, true, now, now);
    }

    public static Pipeline reconstitute(UUID id, UUID companyId, String name, String description,
                                        boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Pipeline(id, companyId, name, description, active, createdAt, updatedAt);
    }

    public void update(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new PipelineValidationException("Nome do pipeline é obrigatório.");
        }
        this.name = name.trim();
        this.description = description;
        touch();
    }

    public void deactivate() {
        this.active = false;
        touch();
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
