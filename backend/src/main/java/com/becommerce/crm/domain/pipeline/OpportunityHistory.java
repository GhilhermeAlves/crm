package com.becommerce.crm.domain.pipeline;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro imutável de movimento de uma oportunidade entre estágios
 * (P-024). Corresponde à tabela {@code opportunity_history} (V017). Cada
 * alteração de estágio gera exatamente um registro; a movimentação é
 * adicionada (insert-only), nunca alterada/removida.
 */
public class OpportunityHistory {

    private final UUID id;
    private final UUID opportunityId;
    private final UUID fromStageId;
    private final UUID toStageId;
    private final UUID changedBy;
    private final LocalDateTime changedAt;
    private final String note;

    private OpportunityHistory(UUID id, UUID opportunityId, UUID fromStageId, UUID toStageId,
                               UUID changedBy, LocalDateTime changedAt, String note) {
        this.id = id;
        this.opportunityId = opportunityId;
        this.fromStageId = fromStageId;
        this.toStageId = toStageId;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.note = note;
    }

    public static OpportunityHistory create(UUID opportunityId, UUID fromStageId, UUID toStageId,
                                            UUID changedBy, String note) {
        return new OpportunityHistory(UUID.randomUUID(), opportunityId, fromStageId, toStageId,
                changedBy, LocalDateTime.now(), note);
    }

    public static OpportunityHistory reconstitute(UUID id, UUID opportunityId, UUID fromStageId,
                                                  UUID toStageId, UUID changedBy, LocalDateTime changedAt,
                                                  String note) {
        return new OpportunityHistory(id, opportunityId, fromStageId, toStageId, changedBy, changedAt, note);
    }

    public UUID getId() { return id; }
    public UUID getOpportunityId() { return opportunityId; }
    public UUID getFromStageId() { return fromStageId; }
    public UUID getToStageId() { return toStageId; }
    public UUID getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getNote() { return note; }
}
