package com.becommerce.crm.domain.lead;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lead por empresa (Sprint 10). Corresponde à tabela {@code leads} (V016),
 * já protegida por RLS (V021). Um lead referencia sempre um contato
 * ({@code contact_id} NOT NULL) e é único por {@code (contact_id, company_id)}
 * (índice único). Award: score em {@code 0..100}, status e source com valores
 * restritos (CHECK no banco).
 */
public class Lead {

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private LeadStatus status;
    private int score;
    private LeadClassification classification;
    private final LeadSource source;
    private UUID campaignId;
    private UUID assignedTo;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Lead(UUID id, UUID companyId, UUID contactId, LeadStatus status, int score,
                 LeadClassification classification, LeadSource source, UUID campaignId,
                 UUID assignedTo, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.status = status;
        this.score = score;
        this.classification = classification;
        this.source = source;
        this.campaignId = campaignId;
        this.assignedTo = assignedTo;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Lead create(UUID companyId, UUID contactId, LeadSource source,
                              LeadStatus status, int score, LeadClassification classification,
                              UUID campaignId, UUID assignedTo, String notes) {
        LocalDateTime now = LocalDateTime.now();
        return new Lead(UUID.randomUUID(), companyId, contactId,
                status != null ? status : LeadStatus.NEW,
                score, classification, source, campaignId, assignedTo, notes, now, now);
    }

    public static Lead reconstitute(UUID id, UUID companyId, UUID contactId,
                                    LeadStatus status, int score, LeadClassification classification,
                                    LeadSource source, UUID campaignId, UUID assignedTo,
                                    String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Lead(id, companyId, contactId, status, score, classification,
                source, campaignId, assignedTo, notes, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public LeadStatus getStatus() { return status; }
    public int getScore() { return score; }
    public LeadClassification getClassification() { return classification; }
    public LeadSource getSource() { return source; }
    public UUID getCampaignId() { return campaignId; }
    public UUID getAssignedTo() { return assignedTo; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Transição de estado (L-010). Um lead {@code CONVERTED} não pode ter o
     * estado alterado (L-023 — não pode ser reconvertido); permitimos apenas
     * reabrir um {@code DISQUALIFIED}/{@code LOST} de volta para {@code NEW}
     * (L-012). {@code UNQUALIFIED} é redundante com a abertura de estado.
     */
    public void transitionTo(LeadStatus newStatus) {
        if (status == LeadStatus.CONVERTED) {
            throw new IllegalStateException("Lead já convertido em oportunidade não pode ter o status alterado.");
        }
        Integer allowedNew = allowedTransition(status);
        if (allowedNew != null && !allowedNew.equals(newStatus)) {
            throw new IllegalStateException("Transição de status inválida de " + status + " para " + newStatus + ".");
        }
        this.status = newStatus;
    }

    private static Integer allowedTransition(LeadStatus current) {
        // Restrições mínimas de escopo Sprint 10: apenas LOST só reabre para NEW.
        switch (current) {
            case LOST: return LeadStatus.NEW.ordinal();
            default: return null; // demais transições livres nesta sprint
        }
    }

    public void updateScore(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score deve estar entre 0 e 100.");
        }
        this.score = score;
    }

    public void updateClassification(LeadClassification classification) {
        this.classification = classification;
    }

    public void assignTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}