package com.becommerce.crm.domain.activity;

import com.becommerce.crm.domain.activity.exception.ActivityValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Activity (Sprint 12) — interação/acontecimento comercial associado a um
 * relacionamento. Pertence a uma empresa (company_id, RLS FORCE) e pode estar
 * vinculada opcionalmente a um contato e/ou a uma oportunidade da MESMA empresa
 * (validado no serviço). Corresponde à tabela {@code activities} (V039).
 *
 * <p>O vínculo contact/opportunity é opcional e não-enumerado por design: permite
 * uma timeline unificada por contato, por oportunidade ou por empresa, e a futura
 * ingestão de eventos do Inbox sem alterar o modelo.</p>
 */
public class Activity {

    private final UUID id;
    private final UUID companyId;
    private UUID contactId;
    private UUID opportunityId;
    private ActivityType type;
    private String subject;
    private String description;
    private LocalDateTime activityAt;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Activity(UUID id, UUID companyId, UUID contactId, UUID opportunityId, ActivityType type,
                     String subject, String description, LocalDateTime activityAt, UUID createdBy,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.opportunityId = opportunityId;
        this.type = type;
        this.subject = subject;
        this.description = description;
        this.activityAt = activityAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Activity create(UUID companyId, UUID contactId, UUID opportunityId, ActivityType type,
                                  String subject, String description, LocalDateTime activityAt, UUID createdBy) {
        if (type == null) {
            throw new ActivityValidationException("O tipo da atividade é obrigatório.");
        }
        if (subject == null || subject.isBlank()) {
            throw new ActivityValidationException("O assunto da atividade é obrigatório.");
        }
        String normalized = subject.trim();
        if (normalized.length() > 255) {
            throw new ActivityValidationException("O assunto deve ter no máximo 255 caracteres.");
        }
        LocalDateTime when = activityAt != null ? activityAt : LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        return new Activity(UUID.randomUUID(), companyId, contactId, opportunityId, type,
                normalized, description, when, createdBy, now, now);
    }

    public static Activity reconstitute(UUID id, UUID companyId, UUID contactId, UUID opportunityId,
                                        ActivityType type, String subject, String description,
                                        LocalDateTime activityAt, UUID createdBy,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Activity(id, companyId, contactId, opportunityId, type, subject, description,
                activityAt, createdBy, createdAt, updatedAt);
    }

    public void update(ActivityType type, String subject, String description, LocalDateTime activityAt) {
        if (type == null) {
            throw new ActivityValidationException("O tipo da atividade é obrigatório.");
        }
        if (subject != null && !subject.isBlank()) {
            this.subject = subject.trim();
        }
        this.type = type;
        this.description = description;
        this.activityAt = activityAt != null ? activityAt : this.activityAt;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getOpportunityId() { return opportunityId; }
    public ActivityType getType() { return type; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public LocalDateTime getActivityAt() { return activityAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}