package com.becommerce.crm.domain.campaign;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Campanha por empresa (Sprint 17), tabela {@code campaigns} (V056, RLS FORCE).
 * Ciclo de vida controlado:
 * <pre>
 * DRAFT -> SCHEDULED -> RUNNING <-> PAUSED -> COMPLETED
 *   \-> CANCELLED          \--------\-> CANCELLED
 * </pre>
 */
public class Campaign {

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String description;
    private CampaignStatus status;
    private final AudienceType audienceType;
    private String audienceCriteria;
    private int estimatedRecipients;
    private LocalDateTime scheduledAt;
    private final String timezone;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Campaign(UUID id, UUID companyId, String name, String description,
                     CampaignStatus status, AudienceType audienceType, String audienceCriteria,
                     int estimatedRecipients, LocalDateTime scheduledAt, String timezone,
                     LocalDateTime startedAt, LocalDateTime completedAt, UUID createdBy,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.audienceType = audienceType;
        this.audienceCriteria = audienceCriteria;
        this.estimatedRecipients = estimatedRecipients;
        this.scheduledAt = scheduledAt;
        this.timezone = timezone;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Campaign create(UUID companyId, String name, String description,
                                  AudienceType audienceType, String audienceCriteria,
                                  String timezone, UUID createdBy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome da campanha é obrigatório.");
        }
        if (audienceType == null) {
            throw new IllegalArgumentException("Tipo de público é obrigatório.");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Campaign(UUID.randomUUID(), companyId, name, description,
                CampaignStatus.DRAFT, audienceType, audienceCriteria, 0,
                null, timezone != null ? timezone : "America/Sao_Paulo",
                null, null, createdBy, now, now);
    }

    public static Campaign reconstitute(UUID id, UUID companyId, String name, String description,
                                        CampaignStatus status, AudienceType audienceType,
                                        String audienceCriteria, int estimatedRecipients,
                                        LocalDateTime scheduledAt, String timezone,
                                        LocalDateTime startedAt, LocalDateTime completedAt,
                                        UUID createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Campaign(id, companyId, name, description, status, audienceType,
                audienceCriteria, estimatedRecipients, scheduledAt, timezone,
                startedAt, completedAt, createdBy, createdAt, updatedAt);
    }

    /**
     * Transição controlada de estado (PLAN.md seção 4). Transições inválidas
     * lançam exceção — o status nunca é alterado diretamente.
     */
    public void transitionTo(CampaignStatus newStatus) {
        if (!allowedTransitions().containsKey(newStatus)) {
            throw new IllegalStateException(
                    "Transição de status inválida de " + status + " para " + newStatus + ".");
        }
        if (newStatus == CampaignStatus.RUNNING) {
            this.startedAt = LocalDateTime.now();
        }
        if (newStatus == CampaignStatus.COMPLETED || newStatus == CampaignStatus.CANCELLED) {
            this.completedAt = LocalDateTime.now();
        }
        this.status = newStatus;
        touch();
    }

    private Map<CampaignStatus, Boolean> allowedTransitions() {
        return switch (status) {
            case DRAFT -> Map.of(CampaignStatus.SCHEDULED, true, CampaignStatus.CANCELLED, true);
            case SCHEDULED -> Map.of(CampaignStatus.RUNNING, true, CampaignStatus.CANCELLED, true);
            case RUNNING -> Map.of(CampaignStatus.PAUSED, true, CampaignStatus.COMPLETED, true,
                    CampaignStatus.CANCELLED, true);
            case PAUSED -> Map.of(CampaignStatus.RUNNING, true, CampaignStatus.CANCELLED, true);
            case COMPLETED, CANCELLED -> Map.of();
        };
    }

    public boolean isEditable() {
        return status == CampaignStatus.DRAFT;
    }

    public void updateDetails(String name, String description) {
        if (!isEditable()) {
            throw new IllegalStateException("Somente campanhas em DRAFT podem ser editadas.");
        }
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        this.description = description;
        touch();
    }

    public void updateAudience(AudienceType audienceType, String audienceCriteria) {
        if (!isEditable()) {
            throw new IllegalStateException("Somente campanhas em DRAFT podem ter o público alterado.");
        }
        this.audienceCriteria = audienceCriteria;
        touch();
    }

    public void setEstimatedRecipients(int estimatedRecipients) {
        this.estimatedRecipients = Math.max(0, estimatedRecipients);
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CampaignStatus getStatus() { return status; }
    public AudienceType getAudienceType() { return audienceType; }
    public String getAudienceCriteria() { return audienceCriteria; }
    public int getEstimatedRecipients() { return estimatedRecipients; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getTimezone() { return timezone; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
