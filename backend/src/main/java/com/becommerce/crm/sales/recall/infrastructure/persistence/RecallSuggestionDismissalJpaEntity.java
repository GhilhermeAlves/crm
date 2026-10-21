package com.becommerce.crm.sales.recall.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recall_suggestion_dismissals")
public class RecallSuggestionDismissalJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    public static RecallSuggestionDismissalJpaEntity create(UUID companyId, UUID contactId, UUID createdBy) {
        RecallSuggestionDismissalJpaEntity entity = new RecallSuggestionDismissalJpaEntity();
        entity.id = UUID.randomUUID();
        entity.companyId = companyId;
        entity.contactId = contactId;
        entity.createdBy = createdBy;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
