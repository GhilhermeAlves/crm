package com.becommerce.crm.finance.patientbilling;

import com.becommerce.crm.billing.application.BillingProperties;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppConnectionService;
import com.becommerce.crm.finance.patientbilling.PatientChargeStore.PatientCharge;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientBillingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneOffset.UTC);
    private final UUID companyId = UUID.randomUUID();
    private final UUID txId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    private final PatientChargeStore store = mock(PatientChargeStore.class);
    private final AsaasClient asaas = mock(AsaasClient.class);
    private final FinancialTransactionRepository transactions = mock(FinancialTransactionRepository.class);
    private final FinancialTransactionService transactionService = mock(FinancialTransactionService.class);
    private final ContactRepository contacts = mock(ContactRepository.class);
    private final WhatsAppConnectionService whatsapp = mock(WhatsAppConnectionService.class);
    private final FinancialTransaction tx = mock(FinancialTransaction.class);
    private final Contact contact = mock(Contact.class);
    private PatientBillingService service;

    @BeforeEach
    void setUp() {
        service = new PatientBillingService(store, asaas, new BillingProperties(true, "u", "KEY", "W"),
                transactions, transactionService, contacts, whatsapp, CLOCK);
        when(store.enabled(companyId)).thenReturn(true);
        when(transactions.findById(txId)).thenReturn(Optional.of(tx));
        when(tx.getId()).thenReturn(txId);
        when(tx.getCompanyId()).thenReturn(companyId);
        when(tx.getType()).thenReturn(TransactionType.INCOME);
        when(tx.getStatus()).thenReturn(TransactionStatus.PENDING);
        when(tx.getContactId()).thenReturn(contactId);
        when(tx.getAmountCents()).thenReturn(500L);
        when(tx.getDueDate()).thenReturn(LocalDate.of(2026, 11, 25));
        when(tx.getDescription()).thenReturn("Orçamento: limpeza");
        when(contacts.findById(contactId)).thenReturn(Optional.of(contact));
        when(contact.getId()).thenReturn(contactId);
        when(contact.getFirstName()).thenReturn("Maria");
        when(contact.getCpf()).thenReturn("123.456.789-09");
        when(contact.getMobile()).thenReturn("34999990000");
        when(store.find(companyId, txId)).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void chargeCreatesCustomerPaymentAndSendsWhatsApp() {
        when(store.customerId(companyId, contactId)).thenReturn(Optional.empty());
        when(asaas.createPatientCustomer(eq("Maria"), any(), any(), any(), any())).thenReturn("cus_1");
        when(asaas.createPayment("cus_1", 500L, LocalDate.of(2026, 11, 25), "Orçamento: limpeza",
                PatientBillingService.ref(companyId, txId)))
                .thenReturn(new AsaasClient.CreatedPayment("pay_1", "https://i/1"));

        var view = service.charge(companyId, txId);

        assertEquals("PENDING", view.status());
        assertEquals("https://i/1", view.invoiceUrl());
        assertTrue(view.whatsappSent());
        verify(store).saveCustomer(companyId, contactId, "cus_1");
        verify(whatsapp).sendText(eq(companyId), eq("34999990000"), contains("https://i/1"));
    }

    @Test
    void chargeReusesCustomerAndUsesTodayWhenOverdue() {
        when(tx.getDueDate()).thenReturn(LocalDate.of(2026, 11, 1));
        when(store.customerId(companyId, contactId)).thenReturn(Optional.of("cus_9"));
        when(asaas.createPayment(eq("cus_9"), eq(500L), eq(LocalDate.of(2026, 11, 18)), any(), any()))
                .thenReturn(new AsaasClient.CreatedPayment("pay_2", "https://i/2"));

        service.charge(companyId, txId);

        verify(asaas, never()).createPatientCustomer(any(), any(), any(), any(), any());
    }

    @Test
    void chargeWithoutCpfIsRejected() {
        when(contact.getCpf()).thenReturn(null);
        assertThrows(PatientBillingException.class, () -> service.charge(companyId, txId));
        verifyNoInteractions(asaas);
    }

    @Test
    void chargeWhenDisabledIsRejected() {
        when(store.enabled(companyId)).thenReturn(false);
        assertThrows(PatientBillingException.class, () -> service.charge(companyId, txId));
    }

    @Test
    void asaasFailureIsStoredAsFailed() {
        when(store.customerId(companyId, contactId)).thenReturn(Optional.of("cus_1"));
        when(asaas.createPayment(any(), anyLong(), any(), any(), any()))
                .thenThrow(new AsaasClient.AsaasException("Asaas indisponível."));

        var view = service.charge(companyId, txId);

        assertEquals("FAILED", view.status());
        verify(store).upsert(argThat(c -> "FAILED".equals(c.status())));
        verifyNoInteractions(whatsapp);
    }

    @Test
    void whatsappFailureDoesNotUndoCharge() {
        when(store.customerId(companyId, contactId)).thenReturn(Optional.of("cus_1"));
        when(asaas.createPayment(any(), anyLong(), any(), any(), any()))
                .thenReturn(new AsaasClient.CreatedPayment("pay_1", "https://i/1"));
        doThrow(new RuntimeException("WhatsApp da clínica não conectado.")).when(whatsapp)
                .sendText(any(), any(), any());

        var view = service.charge(companyId, txId);

        assertEquals("PENDING", view.status());
        assertFalse(view.whatsappSent());
        assertEquals("WhatsApp da clínica não conectado.", view.whatsappError());
    }

    @Test
    void paidInClinicConfirmsCashOnlyWhenPending() {
        when(store.find(companyId, txId)).thenReturn(Optional.of(
                new PatientCharge(txId, companyId, "pay_1", "u", "PENDING", null)));
        when(tx.getPaidAt()).thenReturn(LocalDate.of(2026, 11, 18));

        service.onPaidInClinic(companyId, txId);

        verify(asaas).receiveInCash("pay_1", LocalDate.of(2026, 11, 18), 500L);
        verify(store).setStatus(companyId, txId, "RECEIVED_IN_CASH", null);
    }

    @Test
    void paidByWebhookDoesNotCallReceiveInCash() {
        when(store.find(companyId, txId)).thenReturn(Optional.of(
                new PatientCharge(txId, companyId, "pay_1", "u", "RECEIVED", null)));
        service.onPaidInClinic(companyId, txId);
        verifyNoInteractions(asaas);
    }

    @Test
    void cancelledRemovesPendingCharge() {
        when(store.find(companyId, txId)).thenReturn(Optional.of(
                new PatientCharge(txId, companyId, "pay_1", "u", "PENDING", null)));
        service.onCancelled(companyId, txId);
        verify(asaas).deletePayment("pay_1");
        verify(store).setStatus(companyId, txId, "CANCELLED", null);
    }

    @Test
    void webhookReceivedMarksChargeThenPaysTransaction() {
        when(store.find(companyId, txId)).thenReturn(Optional.of(
                new PatientCharge(txId, companyId, "pay_1", "u", "PENDING", null)));
        var payment = new AsaasClient.AsaasPayment("pay_1", null, PatientBillingService.ref(companyId, txId),
                "RECEIVED", false, LocalDate.of(2026, 11, 25), 500, "u", LocalDate.of(2026, 11, 18));

        assertTrue(service.applyWebhook(payment));

        var order = inOrder(store, transactionService);
        order.verify(store).setStatus(companyId, txId, "RECEIVED", null);
        order.verify(transactionService).pay(eq(companyId), eq(txId), any(), isNull());
    }

    @Test
    void webhookRepeatedIsIdempotent() {
        when(store.find(companyId, txId)).thenReturn(Optional.of(
                new PatientCharge(txId, companyId, "pay_1", "u", "RECEIVED", null)));
        var payment = new AsaasClient.AsaasPayment("pay_1", null, PatientBillingService.ref(companyId, txId),
                "RECEIVED", false, LocalDate.of(2026, 11, 25), 500, "u", LocalDate.of(2026, 11, 18));

        assertTrue(service.applyWebhook(payment));
        verifyNoInteractions(transactionService);
    }

    @Test
    void licensePaymentIsNotHandled() {
        var payment = new AsaasClient.AsaasPayment("pay_1", "sub_1", companyId.toString(),
                "RECEIVED", false, LocalDate.of(2026, 11, 25), 500, "u", null);
        assertFalse(service.applyWebhook(payment));
    }
}
