package com.becommerce.crm.domain.pipeline;

import com.becommerce.crm.domain.pipeline.exception.PipelineValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Oportunidade no pipeline (Sprint 11). Corresponde à tabela {@code
 * opportunities} (V017), protegida por RLS. Representa um negócio em andamento
 * vinculado a um contato (P-010/P-011) e posicionado em um estágio. Fluxo de
 * vida: OPEN → (move por estágios) → WON/LOST (P-020/P-021).
 */
public class Opportunity {

    private final UUID id;
    private final UUID companyId;
    private String title;
    private BigDecimal value;
    private final UUID contactId;
    private final UUID pipelineId;
    private UUID stageId;
    private UUID assignedTo;
    private LocalDateTime expectedCloseDate;
    private OpportunityStatus status;
    private LocalDateTime wonAt;
    private LocalDateTime lostAt;
    private String lossReason;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Opportunity(UUID id, UUID companyId, String title, BigDecimal value, UUID contactId,
                        UUID pipelineId, UUID stageId, UUID assignedTo, LocalDateTime expectedCloseDate,
                        OpportunityStatus status, LocalDateTime wonAt, LocalDateTime lostAt,
                        String lossReason, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.value = value;
        this.contactId = contactId;
        this.pipelineId = pipelineId;
        this.stageId = stageId;
        this.assignedTo = assignedTo;
        this.expectedCloseDate = expectedCloseDate;
        this.status = status;
        this.wonAt = wonAt;
        this.lostAt = lostAt;
        this.lossReason = lossReason;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Opportunity create(UUID companyId, String title, BigDecimal value, UUID contactId,
                                     UUID pipelineId, UUID stageId, UUID assignedTo,
                                     LocalDateTime expectedCloseDate, String notes) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PipelineValidationException("Valor da oportunidade deve ser maior que zero.");
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal normalized = value.setScale(2, java.math.RoundingMode.HALF_UP);
        return new Opportunity(UUID.randomUUID(), companyId, title, normalized, contactId,
                pipelineId, stageId, assignedTo, expectedCloseDate, OpportunityStatus.OPEN,
                null, null, null, notes, now, now);
    }

    public static Opportunity reconstitute(UUID id, UUID companyId, String title, BigDecimal value,
                                           UUID contactId, UUID pipelineId, UUID stageId, UUID assignedTo,
                                           LocalDateTime expectedCloseDate, OpportunityStatus status,
                                           LocalDateTime wonAt, LocalDateTime lostAt, String lossReason,
                                           String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Opportunity(id, companyId, title, value, contactId, pipelineId, stageId, assignedTo,
                expectedCloseDate, status, wonAt, lostAt, lossReason, notes, createdAt, updatedAt);
    }

    public void update(String title, BigDecimal value, UUID assignedTo,
                       LocalDateTime expectedCloseDate, String notes) {
        requireOpen();
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PipelineValidationException("Valor da oportunidade deve ser maior que zero.");
        }
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        this.value = value.setScale(2, java.math.RoundingMode.HALF_UP);
        if (assignedTo != null) {
            this.assignedTo = assignedTo;
        }
        if (expectedCloseDate != null) {
            this.expectedCloseDate = expectedCloseDate;
        }
        if (notes != null) {
            this.notes = notes;
        }
        touch();
    }

    /**
     * Move a oportunidade para outro estágio (P-020). A regra de vizinhança
     * (±1 estágio) é validada pelo serviço, que conhece a ordem do funil.
     * Oportunidades ganhas/perdidas são imutáveis (P-024).
     */
    public void moveTo(UUID newStageId) {
        requireOpen();
        this.stageId = newStageId;
        touch();
    }

    public void markWon(LocalDateTime when) {
        requireOpen();
        this.status = OpportunityStatus.WON;
        this.wonAt = when;
        this.lostAt = null;
        this.lossReason = null;
        touch();
    }

    /**
     * Perde a oportunidade. O motivo é obrigatório (P-022) e validado no
     * serviço antes; aqui apenas garantimos que não seja reaberta.
     */
    public void markLost(String lossReason, LocalDateTime when) {
        requireOpen();
        this.status = OpportunityStatus.LOST;
        this.wonAt = null;
        this.lostAt = when;
        this.lossReason = lossReason;
        touch();
    }

    private void requireOpen() {
        if (status != OpportunityStatus.OPEN) {
            throw new PipelineValidationException(
                    "Oportunidade " + status + " é imutável (não pode ser alterada).");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public BigDecimal getValue() { return value; }
    public UUID getContactId() { return contactId; }
    public UUID getPipelineId() { return pipelineId; }
    public UUID getStageId() { return stageId; }
    public UUID getAssignedTo() { return assignedTo; }
    public LocalDateTime getExpectedCloseDate() { return expectedCloseDate; }
    public OpportunityStatus getStatus() { return status; }
    public LocalDateTime getWonAt() { return wonAt; }
    public LocalDateTime getLostAt() { return lostAt; }
    public String getLossReason() { return lossReason; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
