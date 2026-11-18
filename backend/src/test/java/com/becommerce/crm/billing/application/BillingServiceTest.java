package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingInvoice;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.domain.InvoiceStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.billing.infrastructure.BillingInvoiceJpaRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
    private final UUID companyId = UUID.randomUUID();

    @Mock BillingAccountJpaRepository accounts;
    @Mock BillingInvoiceJpaRepository invoices;
    @Mock BillingPaymentService payments;
    @Mock BillingAccessService access;
    @Mock CompanyRepository companies;
    @Mock AsaasClient asaas;
    @Mock Company company;
    BillingService service;

    @BeforeEach
    void setUp() {
        service = new BillingService(accounts, invoices, companies, asaas, payments, access,
                new BillingProperties(true, "https://x", "KEY", "WH"), CLOCK);
        lenient().when(companies.findById(companyId)).thenReturn(Optional.of(company));
        lenient().when(company.getPlan()).thenReturn(CompanyPlan.STARTER);
        lenient().when(company.getTradingName()).thenReturn("Clínica Sorriso");
        lenient().when(company.getEmail()).thenReturn("c@s.com");
        lenient().when(company.getCnpj()).thenReturn("123.456.789-09");
        lenient().when(accounts.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void activateRestoresCallerTenantContext() {
        UUID other = UUID.randomUUID();
        TenantContext.setCompanyId(other);
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer(any(), any(), any(), any())).thenReturn("cus_1");
        when(asaas.createSubscription(any(), anyInt(), any(), anyString(), anyString())).thenReturn("sub_1");

        service.activate(companyId);

        assertEquals(other, TenantContext.getCompanyId());
    }

    @Test
    void activateCreatesCustomerAndPixSubscriptionDueIn7Days() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer("Clínica Sorriso", "123.456.789-09", "c@s.com", companyId.toString())).thenReturn("cus_1");
        when(asaas.createSubscription(eq("cus_1"), eq(19700), eq(LocalDate.of(2026, 11, 15)), anyString(), eq(companyId.toString())))
                .thenReturn("sub_1");

        var view = service.activate(companyId);

        assertEquals("ACTIVE", view.status());
        assertEquals(LocalDate.of(2026, 11, 15), view.nextDueDate());
    }

    @Test
    void activateWithoutDocumentIsRejected() {
        when(company.getCnpj()).thenReturn(null);
        assertThrows(BillingRuleException.class, () -> service.activate(companyId));
        verifyNoInteractions(asaas);
    }

    @Test
    void activateWhenDisabledIsRejected() {
        service = new BillingService(accounts, invoices, companies, asaas, payments, access, new BillingProperties(false, "https://x", "", ""), CLOCK);
        assertThrows(BillingRuleException.class, () -> service.activate(companyId));
        verifyNoInteractions(asaas);
    }

    @Test
    void asaasFailureMarksSyncPendingWithoutThrowing() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer(any(), any(), any(), any())).thenThrow(new AsaasClient.AsaasException("Asaas indisponível."));

        var view = service.activate(companyId);

        assertEquals("SYNC_PENDING", view.status());
        assertEquals("Asaas indisponível.", view.syncError());
    }

    @Test
    void retryReusesExistingCustomer() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.customerCreated("cus_1");
        acc.syncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        when(asaas.createSubscription(eq("cus_1"), anyInt(), any(), anyString(), anyString())).thenReturn("sub_1");

        assertEquals("ACTIVE", service.activate(companyId).status());
        verify(asaas, never()).createCustomer(any(), any(), any(), any());
    }

    @Test
    void activateIsIdempotentWhenSubscriptionExists() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.activate(companyId);
        verifyNoInteractions(asaas);
    }

    @Test
    void planChangeUpdatesDefaultPriceAndAsaas() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.onPlanChanged(companyId, CompanyPlan.BUSINESS);

        assertEquals(79700, acc.getMonthlyPriceCents());
        verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(79700), anyString());
    }

    @Test
    void planChangeReplacesNegotiatedPriceWithNewPlanPrice() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 15000);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.onPlanChanged(companyId, CompanyPlan.BUSINESS);

        assertEquals(79700, acc.getMonthlyPriceCents());
        assertEquals(CompanyPlan.BUSINESS, acc.getPlan());
        verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(79700), anyString());
    }

    @Test
    void planChangeWithoutSubscriptionOnlyUpdatesLocally() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 15000);
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.onPlanChanged(companyId, CompanyPlan.BUSINESS);

        assertEquals(79700, acc.getMonthlyPriceCents());
        verifyNoInteractions(asaas);
    }

    @Test
    void exemptWithoutAccountBecomesExempt() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        assertEquals("EXEMPT", service.markExempt(companyId).status());
    }

    @Test
    void exemptWithSubscriptionIsRejectedWithoutSaving() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        assertThrows(BillingRuleException.class, () -> service.markExempt(companyId));
        verifyNoInteractions(asaas);
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void exemptCompanyCannotBeActivated() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.markExempt();
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        assertThrows(BillingRuleException.class, () -> service.activate(companyId));
    }

    @Test
    void changePriceFailureKeepsStatusAndRecordsError() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        doThrow(new AsaasClient.AsaasException("Asaas indisponível.")).when(asaas).updateSubscriptionValue(any(), anyInt(), any());

        var view = service.changePrice(companyId, 17000);

        assertEquals("ACTIVE", view.status());
        assertEquals(17000, view.monthlyPriceCents());
        assertEquals("Asaas indisponível.", view.syncError());
    }

    @Test
    void retrySyncReactivatesPendingAccount() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.customerCreated("cus_1");
        acc.syncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        when(asaas.createSubscription(eq("cus_1"), anyInt(), any(), anyString(), anyString())).thenReturn("sub_1");

        service.retrySync(companyId);

        assertEquals(BillingStatus.ACTIVE, acc.getStatus());
    }

    @Test
    void retrySyncResendsPendingPrice() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        acc.priceSyncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.retrySync(companyId);

        verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(19700), anyString());
        assertNull(acc.getSyncError());
    }

    @Test
    void grantUnblocksForNDays() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 10, 15));
        acc.onOverdue(LocalDate.of(2026, 10, 15));
        acc.block();
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        var view = service.grant(companyId, 7);

        assertEquals("PAST_DUE", view.status());
        assertEquals(LocalDate.of(2026, 11, 15), view.graceUntil());
        verify(access).evict(companyId);
    }

    @Test
    void grantOutOfRangeIsRejected() {
        assertThrows(BillingRuleException.class, () -> service.grant(companyId, 0));
        assertThrows(BillingRuleException.class, () -> service.grant(companyId, 31));
    }

    @Test
    void cancelDeletesSubscriptionCancelsOpenInvoicesAndResets() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 10, 15));
        acc.onOverdue(LocalDate.of(2026, 10, 15));
        acc.block();
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        BillingInvoice open = BillingInvoice.fromAsaas(companyId, "pay_1", LocalDate.of(2026, 10, 15), 19700,
                InvoiceStatus.OVERDUE, "https://pix/1", null);
        when(invoices.findByCompanyIdAndStatusIn(eq(companyId), any())).thenReturn(List.of(open), List.of());

        var view = service.cancel(companyId);

        verify(asaas).deleteSubscription("sub_1");
        assertEquals(InvoiceStatus.CANCELLED, open.getStatus());
        assertEquals("NOT_STARTED", view.status());
        assertFalse(view.hasSubscription());
        assertNull(view.overdueSince());
        assertNull(view.nextDueDate());
        assertEquals("cus_1", acc.getAsaasCustomerId());
        verify(access).evict(companyId);
    }

    @Test
    void cancelFailureInAsaasChangesNothing() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        doThrow(new AsaasClient.AsaasException("Asaas indisponível.")).when(asaas).deleteSubscription("sub_1");

        assertThrows(BillingRuleException.class, () -> service.cancel(companyId));

        assertEquals(BillingStatus.ACTIVE, acc.getStatus());
        assertEquals("sub_1", acc.getAsaasSubscriptionId());
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void cancelWithoutSubscriptionIsRejected() {
        when(accounts.findById(companyId)).thenReturn(Optional.of(BillingAccount.start(companyId, CompanyPlan.STARTER, 19700)));
        assertThrows(BillingRuleException.class, () -> service.cancel(companyId));
        verifyNoInteractions(asaas);
    }

    @Test
    void cancelSyncPendingResetsWithoutCallingAsaas() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.syncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        assertEquals("NOT_STARTED", service.cancel(companyId).status());
        verifyNoInteractions(asaas);
    }

    @Test
    void checkPaymentRefreshesOpenInvoices() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.checkPayment(companyId);

        verify(payments).refreshOpen(companyId, null);
        verify(access).evict(companyId);
    }

    @Test
    void checkPaymentAsksAsaasAtMostOncePer30Seconds() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.checkPayment(companyId);
        service.checkPayment(companyId);

        verify(payments, times(1)).refreshOpen(companyId, null);
    }

    @Test
    void firstDueDateUsesSaoPauloDateLateAtNight() {
        // 01:00 UTC de 09/11 = 22:00 de 08/11 em São Paulo: vencimento conta a partir de 08/11.
        Clock night = Clock.fixed(Instant.parse("2026-11-09T01:00:00Z"), ZoneOffset.UTC);
        BillingService s = new BillingService(accounts, invoices, companies, asaas, payments, access,
                new BillingProperties(true, "https://x", "KEY", "WH"), night);
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer(any(), any(), any(), any())).thenReturn("cus_1");
        when(asaas.createSubscription(eq("cus_1"), eq(19700), eq(LocalDate.of(2026, 11, 15)), anyString(), anyString()))
                .thenReturn("sub_1");

        assertEquals(LocalDate.of(2026, 11, 15), s.activate(companyId).nextDueDate());
    }

    @Test
    void customerIsSavedEvenWhenSubscriptionFails() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer(any(), any(), any(), any())).thenReturn("cus_1");
        when(asaas.createSubscription(any(), anyInt(), any(), anyString(), anyString()))
                .thenThrow(new AsaasClient.AsaasException("Asaas indisponível."));

        var view = service.activate(companyId);

        assertEquals("SYNC_PENDING", view.status());
        verify(accounts, times(2)).saveAndFlush(argThat(a -> "cus_1".equals(a.getAsaasCustomerId())));
    }
}
