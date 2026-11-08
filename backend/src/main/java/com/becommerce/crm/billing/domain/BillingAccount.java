package com.becommerce.crm.billing.domain;

import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_accounts")
public class BillingAccount {

    private static final int MAX_ERROR = 500;

    @Id @Column(name = "company_id") private UUID companyId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BillingStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CompanyPlan plan;
    @Column(name = "monthly_price_cents", nullable = false) private int monthlyPriceCents;
    @Column(name = "asaas_customer_id") private String asaasCustomerId;
    @Column(name = "asaas_subscription_id") private String asaasSubscriptionId;
    @Column(name = "next_due_date") private LocalDate nextDueDate;
    @Column(name = "overdue_since") private LocalDate overdueSince;
    @Column(name = "grace_until") private LocalDate graceUntil;
    @Column(name = "sync_error") private String syncError;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    protected BillingAccount() {}

    public static BillingAccount start(UUID companyId, CompanyPlan plan, int priceCents) {
        BillingAccount a = new BillingAccount();
        a.companyId = companyId;
        a.status = BillingStatus.NOT_STARTED;
        a.plan = plan;
        a.setPrice(priceCents);
        return a;
    }

    public void activated(String customerId, String subscriptionId, LocalDate nextDue) {
        this.asaasCustomerId = customerId;
        this.asaasSubscriptionId = subscriptionId;
        this.nextDueDate = nextDue;
        this.status = BillingStatus.ACTIVE;
        this.syncError = null;
        touch();
    }

    /** Guarda o cliente criado no Asaas mesmo se a assinatura falhar depois (evita cliente duplicado). */
    public void customerCreated(String customerId) {
        this.asaasCustomerId = customerId;
        touch();
    }

    public void syncFailed(String error) {
        this.status = BillingStatus.SYNC_PENDING;
        String e = error == null ? "erro desconhecido" : error;
        this.syncError = e.length() > MAX_ERROR ? e.substring(0, MAX_ERROR) : e;
        touch();
    }

    /** Valor/plano trocados mas ainda não enviados ao Asaas: mantém o status, registra o erro. */
    public void priceSyncFailed(String error) {
        String e = error == null ? "erro desconhecido" : error;
        this.syncError = e.length() > MAX_ERROR ? e.substring(0, MAX_ERROR) : e;
        touch();
    }

    public void priceSynced() {
        this.syncError = null;
        touch();
    }

    public void markExempt() {
        this.status = BillingStatus.EXEMPT;
        touch();
    }

    public void changePrice(CompanyPlan newPlan, int cents) {
        this.plan = newPlan;
        setPrice(cents);
        touch();
    }

    public boolean isChargeable() { return status != BillingStatus.EXEMPT; }

    public boolean hasSubscription() { return asaasSubscriptionId != null; }

    private void setPrice(int cents) {
        if (cents <= 0) throw new IllegalArgumentException("Valor mensal deve ser maior que zero.");
        this.monthlyPriceCents = cents;
    }

    private void touch() { this.updatedAt = LocalDateTime.now(); }

    public UUID getCompanyId() { return companyId; }
    public BillingStatus getStatus() { return status; }
    public CompanyPlan getPlan() { return plan; }
    public int getMonthlyPriceCents() { return monthlyPriceCents; }
    public String getAsaasCustomerId() { return asaasCustomerId; }
    public String getAsaasSubscriptionId() { return asaasSubscriptionId; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public LocalDate getOverdueSince() { return overdueSince; }
    public LocalDate getGraceUntil() { return graceUntil; }
    public String getSyncError() { return syncError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
