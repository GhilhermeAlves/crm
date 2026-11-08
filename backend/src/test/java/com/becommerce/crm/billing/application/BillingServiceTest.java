package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
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
    @Mock CompanyRepository companies;
    @Mock AsaasClient asaas;
    @Mock Company company;
    BillingService service;

    @BeforeEach
    void setUp() {
        service = new BillingService(accounts, companies, asaas,
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
        service = new BillingService(accounts, companies, asaas, new BillingProperties(false, "https://x", "", ""), CLOCK);
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
    void planChangeKeepsNegotiatedPrice() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 15000);
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.onPlanChanged(companyId, CompanyPlan.BUSINESS);

        assertEquals(15000, acc.getMonthlyPriceCents());
        assertEquals(CompanyPlan.BUSINESS, acc.getPlan());
        verifyNoInteractions(asaas);
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
}
