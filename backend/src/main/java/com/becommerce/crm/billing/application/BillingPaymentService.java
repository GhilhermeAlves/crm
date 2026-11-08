package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingInvoice;
import com.becommerce.crm.billing.domain.InvoiceStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.AsaasClient.AsaasPayment;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.billing.infrastructure.BillingInvoiceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Único caminho que aplica um pagamento: sempre a partir do que o Asaas diz em GET /payments/{id}. */
@Service
public class BillingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BillingPaymentService.class);
    static final EnumSet<InvoiceStatus> OPEN = EnumSet.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

    private final AsaasClient asaas;
    private final BillingAccountJpaRepository accounts;
    private final BillingInvoiceJpaRepository invoices;
    private final BillingAccessService access;

    public BillingPaymentService(AsaasClient asaas, BillingAccountJpaRepository accounts,
                                 BillingInvoiceJpaRepository invoices, BillingAccessService access) {
        this.asaas = asaas;
        this.accounts = accounts;
        this.invoices = invoices;
        this.access = access;
    }

    public Optional<UUID> apply(String asaasPaymentId) {
        AsaasPayment p = asaas.getPayment(asaasPaymentId);
        Optional<UUID> company = companyOf(p);
        if (company.isEmpty()) {
            log.warn("[BILLING] pagamento {} sem empresa reconhecida; ignorado", p.id());
            return Optional.empty();
        }
        UUID companyId = company.get();
        boolean applied = BillingTenant.run(companyId, () -> applyInTenant(companyId, p));
        if (!applied) {
            log.warn("[BILLING] pagamento {} não pertence à assinatura da empresa {}; ignorado", p.id(), companyId);
            return Optional.empty();
        }
        access.evict(companyId);
        return company;
    }

    public int refreshOpen(UUID companyId, LocalDate before) {
        List<BillingInvoice> open = BillingTenant.run(companyId, () -> before == null
                ? invoices.findByCompanyIdAndStatusIn(companyId, OPEN)
                : invoices.findByCompanyIdAndStatusInAndDueDateBefore(companyId, OPEN, before));
        int applied = 0;
        for (BillingInvoice i : open) {
            try {
                if (apply(i.getAsaasPaymentId()).isPresent()) applied++;
            } catch (RuntimeException e) {
                log.warn("[BILLING] falha ao reconsultar pagamento {}: {}", i.getAsaasPaymentId(), e.getMessage());
            }
        }
        return applied;
    }

    private boolean applyInTenant(UUID companyId, AsaasPayment p) {
        Optional<BillingAccount> found = accounts.findById(companyId);
        if (found.isEmpty() || p.subscription() == null
                || !p.subscription().equals(found.get().getAsaasSubscriptionId())) {
            return false;
        }
        BillingAccount acc = found.get();
        InvoiceStatus status = toInvoiceStatus(p);
        LocalDateTime paidAt = (status == InvoiceStatus.RECEIVED || status == InvoiceStatus.CONFIRMED)
                && p.paymentDate() != null ? p.paymentDate().atStartOfDay() : null;
        BillingInvoice invoice = invoices.findByAsaasPaymentId(p.id())
                .map(i -> { i.refresh(p.dueDate(), p.valueCents(), status, p.invoiceUrl(), paidAt); return i; })
                .orElseGet(() -> BillingInvoice.fromAsaas(companyId, p.id(), p.dueDate(), p.valueCents(),
                        status, p.invoiceUrl(), paidAt));
        invoices.saveAndFlush(invoice);

        invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE)
                .ifPresentOrElse(oldest -> acc.onOverdue(oldest.getDueDate()), acc::onPaid);
        if (status == InvoiceStatus.PENDING) acc.nextDueAt(p.dueDate());
        accounts.saveAndFlush(acc);
        return true;
    }

    private Optional<UUID> companyOf(AsaasPayment p) {
        String ref = p.externalReference();
        if (ref == null && p.subscription() != null) ref = asaas.getSubscriptionReference(p.subscription());
        if (ref == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(ref));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static InvoiceStatus toInvoiceStatus(AsaasPayment p) {
        if (p.deleted()) return InvoiceStatus.CANCELLED;
        return switch (p.status() == null ? "" : p.status()) {
            case "RECEIVED", "RECEIVED_IN_CASH" -> InvoiceStatus.RECEIVED;
            case "CONFIRMED" -> InvoiceStatus.CONFIRMED;
            case "OVERDUE" -> InvoiceStatus.OVERDUE;
            case "REFUNDED", "REFUND_REQUESTED", "REFUND_IN_PROGRESS", "CHARGEBACK_REQUESTED",
                 "CHARGEBACK_DISPUTE", "AWAITING_CHARGEBACK_REVERSAL" -> InvoiceStatus.REFUNDED;
            default -> InvoiceStatus.PENDING;
        };
    }
}
