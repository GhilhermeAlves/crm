package com.becommerce.crm.infrastructure.pipeline.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunity_history")
public class OpportunityHistoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    @Column(name = "from_stage_id")
    private UUID fromStageId;

    @Column(name = "to_stage_id")
    private UUID toStageId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "note")
    private String note;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOpportunityId() { return opportunityId; }
    public void setOpportunityId(UUID opportunityId) { this.opportunityId = opportunityId; }
    public UUID getFromStageId() { return fromStageId; }
    public void setFromStageId(UUID fromStageId) { this.fromStageId = fromStageId; }
    public UUID getToStageId() { return toStageId; }
    public void setToStageId(UUID toStageId) { this.toStageId = toStageId; }
    public UUID getChangedBy() { return changedBy; }
    public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
