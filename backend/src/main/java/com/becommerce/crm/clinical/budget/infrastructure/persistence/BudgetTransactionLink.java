package com.becommerce.crm.clinical.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/** Linha de {@code budget_transactions}, coleção de {@link BudgetJpaEntity}. */
@Embeddable
public class BudgetTransactionLink {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    public BudgetTransactionLink() {
    }

    public BudgetTransactionLink(UUID companyId, UUID transactionId) {
        this.companyId = companyId;
        this.transactionId = transactionId;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
}
