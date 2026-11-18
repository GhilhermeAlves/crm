package com.becommerce.crm.billing.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_invoices")
public class BillingInvoice {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "asaas_payment_id", nullable = false, unique = true) private String asaasPaymentId;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "value_cents", nullable = false) private int valueCents;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InvoiceStatus status;
    @Column(name = "invoice_url") private String invoiceUrl;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    protected BillingInvoice() {}

    public static BillingInvoice fromAsaas(UUID companyId, String asaasPaymentId, LocalDate dueDate, int valueCents,
                                           InvoiceStatus status, String invoiceUrl, LocalDateTime paidAt) {
        BillingInvoice i = new BillingInvoice();
        i.id = UUID.randomUUID();
        i.companyId = companyId;
        i.asaasPaymentId = asaasPaymentId;
        i.refresh(dueDate, valueCents, status, invoiceUrl, paidAt);
        return i;
    }

    public void refresh(LocalDate dueDate, int valueCents, InvoiceStatus status, String invoiceUrl,
                        LocalDateTime paidAt) {
        this.dueDate = dueDate;
        this.valueCents = valueCents;
        this.status = status;
        this.invoiceUrl = invoiceUrl;
        this.paidAt = paidAt;
        this.updatedAt = LocalDateTime.now();
    }

    /** Fatura em aberto de uma assinatura encerrada: o Asaas a removeu. */
    public void cancel() {
        if (!isOpen()) return;
        this.status = InvoiceStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.OVERDUE;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getAsaasPaymentId() { return asaasPaymentId; }
    public LocalDate getDueDate() { return dueDate; }
    public int getValueCents() { return valueCents; }
    public InvoiceStatus getStatus() { return status; }
    public String getInvoiceUrl() { return invoiceUrl; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
