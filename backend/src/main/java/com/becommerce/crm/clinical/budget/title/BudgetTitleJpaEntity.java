package com.becommerce.crm.clinical.budget.title;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budget_titles")
public class BudgetTitleJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "title")
    private String title;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected BudgetTitleJpaEntity() {
    }

    public BudgetTitleJpaEntity(UUID companyId, String title, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.title = title;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTitle() {
        return title;
    }
}
