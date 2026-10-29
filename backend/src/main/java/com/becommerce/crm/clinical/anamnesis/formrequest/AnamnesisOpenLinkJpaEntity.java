package com.becommerce.crm.clinical.anamnesis.formrequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** Link aberto (fixo) de um modelo de anamnese (V119). */
@Entity
@Table(name = "anamnesis_open_links")
public class AnamnesisOpenLinkJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "token")
    private String token;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected AnamnesisOpenLinkJpaEntity() {
    }

    public AnamnesisOpenLinkJpaEntity(UUID companyId, UUID modelId, String token, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.modelId = modelId;
        this.token = token;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getModelId() { return modelId; }
    public String getToken() { return token; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
