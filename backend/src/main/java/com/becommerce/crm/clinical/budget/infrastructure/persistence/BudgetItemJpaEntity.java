package com.becommerce.crm.clinical.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "budget_items")
public class BudgetItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "procedure_id")
    private UUID procedureId;

    @Column(name = "price_cents")
    private long priceCents;

    @Column(name = "position")
    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private BudgetJpaEntity budget;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProcedureId() { return procedureId; }
    public void setProcedureId(UUID procedureId) { this.procedureId = procedureId; }
    public long getPriceCents() { return priceCents; }
    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public BudgetJpaEntity getBudget() { return budget; }
    public void setBudget(BudgetJpaEntity budget) { this.budget = budget; }
}
