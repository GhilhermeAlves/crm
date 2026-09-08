package com.becommerce.crm.domain.followup;

import com.becommerce.crm.domain.followup.exception.FollowUpSequenceValidationException;

/**
 * FollowUpSequence — grupo nomeado e reutilizável de follow-ups (Sprint 22).
 *
 * <p>Entidade de domínio pura (POJO), no padrão de {@link FollowUp}: factories
 * estáticas {@code create}/{@code reconstitute} + métodos de mutação que
 * validam. Sem infraestrutura. Scoped à empresa (o {@code companyId} nunca
 * vem do client).
 */
public class FollowUpSequence {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;

    private final java.util.UUID id;
    private final java.util.UUID companyId;
    private String name;
    private String description;
    private FollowUpSequenceStatus status;
    private final java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    private FollowUpSequence(java.util.UUID id, java.util.UUID companyId, String name, String description,
                             FollowUpSequenceStatus status, java.time.LocalDateTime createdAt,
                             java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FollowUpSequence create(java.util.UUID companyId, String name, String description) {
        validateName(name);
        return new FollowUpSequence(java.util.UUID.randomUUID(), companyId, name.trim(),
                trimToNull(description), FollowUpSequenceStatus.ACTIVE,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    public static FollowUpSequence reconstitute(java.util.UUID id, java.util.UUID companyId, String name,
                                                String description, FollowUpSequenceStatus status,
                                                java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        return new FollowUpSequence(id, companyId, name, description, status, createdAt, updatedAt);
    }

    /** Atualiza nome/descrição preservando o id e o tenant. */
    public void update(String name, String description) {
        validateName(name);
        this.name = name.trim();
        this.description = trimToNull(description);
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void activate() {
        this.status = FollowUpSequenceStatus.ACTIVE;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void deactivate() {
        this.status = FollowUpSequenceStatus.INACTIVE;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public boolean isActive() {
        return status == FollowUpSequenceStatus.ACTIVE;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new FollowUpSequenceValidationException("O nome da sequência é obrigatório");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new FollowUpSequenceValidationException(
                    "O nome da sequência excede o limite de " + MAX_NAME_LENGTH + " caracteres");
        }
    }

    private static String trimToNull(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new FollowUpSequenceValidationException(
                    "A descrição excede o limite de " + MAX_DESCRIPTION_LENGTH + " caracteres");
        }
        return trimmed;
    }

    public java.util.UUID getId() { return id; }
    public java.util.UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public FollowUpSequenceStatus getStatus() { return status; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}
