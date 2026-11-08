package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.*;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.AsaasClient.AsaasPayment;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.billing.infrastructure.BillingInvoiceJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingPaymentServiceTest {
    private final UUID companyId = UUID.randomUUID();
    @Mock AsaasClient asaas;
    @Mock BillingAccountJpaRepository accounts;
    @Mock BillingInvoiceJpaRepository invoices;
    @Mock BillingAccessService access;
    BillingPaymentService service;
    BillingAccount acc;

    @BeforeEach
    void setUp() {
        service = new BillingPaymentService(asaas, accounts, invoices, access);
        acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        lenient().when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        lenient().when(invoices.findByAsaasPaymentId(any())).thenReturn(Optional.empty());
        lenient().when(invoices.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(accounts.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    private AsaasPayment payment(String status, String ref) {
        return new AsaasPayment("pay_1", "sub_1", ref, status, false, LocalDate.of(2026, 11, 15), 19700,
                "https://pix/1", "RECEIVED".equals(status) ? LocalDate.of(2026, 11, 20) : null);
    }

    @Test
    void overduePaymentMovesAccountToPastDue() {
        when(asaas.getPayment("pay_1")).thenReturn(payment("OVERDUE", companyId.toString()));
        BillingInvoice overdue = BillingInvoice.fromAsaas(companyId, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.OVERDUE, null, null);
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.of(overdue));

        assertEquals(Optional.of(companyId), service.apply("pay_1"));
        assertEquals(BillingStatus.PAST_DUE, acc.getStatus());
        assertEquals(LocalDate.of(2026, 11, 15), acc.getOverdueSince());
        verify(access).evict(companyId);
    }

    @Test
    void receivedPaymentReactivatesBlockedAccount() {
        acc.onOverdue(LocalDate.of(2026, 11, 15));
        acc.block();
        when(asaas.getPayment("pay_1")).thenReturn(payment("RECEIVED", companyId.toString()));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        service.apply("pay_1");

        assertEquals(BillingStatus.ACTIVE, acc.getStatus());
        verify(invoices).saveAndFlush(argThat(i -> i.getStatus() == InvoiceStatus.RECEIVED && i.getPaidAt() != null));
    }

    @Test
    void paymentOfAnotherSubscriptionIsIgnored() {
        when(asaas.getPayment("pay_1")).thenReturn(new AsaasPayment("pay_1", "sub_X", companyId.toString(),
                "RECEIVED", false, LocalDate.of(2026, 11, 15), 19700, null, LocalDate.of(2026, 11, 20)));

        assertTrue(service.apply("pay_1").isEmpty());
        verify(invoices, never()).saveAndFlush(any());
    }

    @Test
    void missingReferenceFallsBackToSubscription() {
        when(asaas.getPayment("pay_1")).thenReturn(payment("PENDING", null));
        when(asaas.getSubscriptionReference("sub_1")).thenReturn(companyId.toString());
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        assertEquals(Optional.of(companyId), service.apply("pay_1"));
    }

    @Test
    void writesRunInsideTargetTenantAndRestoreCaller() {
        UUID caller = UUID.randomUUID();
        TenantContext.setCompanyId(caller);
        AtomicReference<UUID> seen = new AtomicReference<>();
        when(asaas.getPayment("pay_1")).thenReturn(payment("PENDING", companyId.toString()));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(accounts.saveAndFlush(any())).thenAnswer(i -> { seen.set(TenantContext.getCompanyId()); return i.getArgument(0); });

        service.apply("pay_1");

        assertEquals(companyId, seen.get());
        assertEquals(caller, TenantContext.getCompanyId());
    }

    @Test
    void statusMapping() {
        assertEquals(InvoiceStatus.RECEIVED, BillingPaymentService.toInvoiceStatus(payment("RECEIVED_IN_CASH", null)));
        assertEquals(InvoiceStatus.REFUNDED, BillingPaymentService.toInvoiceStatus(payment("CHARGEBACK_REQUESTED", null)));
        assertEquals(InvoiceStatus.PENDING, BillingPaymentService.toInvoiceStatus(payment("AWAITING_RISK_ANALYSIS", null)));
        assertEquals(InvoiceStatus.CANCELLED, BillingPaymentService.toInvoiceStatus(
                new AsaasPayment("pay_1", "sub_1", null, "PENDING", true, LocalDate.of(2026, 11, 15), 1, null, null)));
    }

    @Test
    void refreshOpenReappliesEachOpenInvoice() {
        BillingInvoice open = BillingInvoice.fromAsaas(companyId, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.PENDING, null, null);
        when(invoices.findByCompanyIdAndStatusInAndDueDateBefore(eq(companyId), anyCollection(), eq(LocalDate.of(2026, 11, 20))))
                .thenReturn(List.of(open));
        when(asaas.getPayment("pay_1")).thenReturn(payment("RECEIVED", companyId.toString()));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        assertEquals(1, service.refreshOpen(companyId, LocalDate.of(2026, 11, 20)));
    }

    @Test
    void paymentWithoutSubscriptionIsIgnoredForAccountWithoutSubscription() {
        BillingAccount bare = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        when(accounts.findById(companyId)).thenReturn(Optional.of(bare));
        when(asaas.getPayment("pay_1")).thenReturn(new AsaasPayment("pay_1", null, companyId.toString(),
                "RECEIVED", false, LocalDate.of(2026, 11, 15), 19700, null, LocalDate.of(2026, 11, 20)));

        assertTrue(service.apply("pay_1").isEmpty());
        verify(invoices, never()).saveAndFlush(any());
    }

    @Test
    void refreshOpenContinuesAfterFailure() {
        BillingInvoice first = BillingInvoice.fromAsaas(companyId, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.PENDING, null, null);
        BillingInvoice second = BillingInvoice.fromAsaas(companyId, "pay_2", LocalDate.of(2026, 11, 16), 19700,
                InvoiceStatus.PENDING, null, null);
        when(invoices.findByCompanyIdAndStatusIn(eq(companyId), anyCollection())).thenReturn(List.of(first, second));
        when(asaas.getPayment("pay_1")).thenThrow(new RuntimeException("falha"));
        when(asaas.getPayment("pay_2")).thenReturn(new AsaasPayment("pay_2", "sub_1", companyId.toString(),
                "RECEIVED", false, LocalDate.of(2026, 11, 16), 19700, null, LocalDate.of(2026, 11, 20)));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        assertEquals(1, service.refreshOpen(companyId, null));
    }
}
