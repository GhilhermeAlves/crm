package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingWebhookEventStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsaasWebhookServiceTest {
    private final BillingPaymentService payments = mock(BillingPaymentService.class);
    private final BillingWebhookEventStore events = mock(BillingWebhookEventStore.class);
    private final AsaasWebhookService service =
            new AsaasWebhookService(payments, events, new BillingProperties(true, "u", "KEY", "WH-TOKEN"));

    private JsonNode body(String event) throws Exception {
        return new ObjectMapper().readTree(
                "{\"id\":\"evt_1\",\"event\":\"" + event + "\",\"payment\":{\"id\":\"pay_1\"}}");
    }

    @Test
    void tokenMustMatch() {
        assertTrue(service.tokenMatches("WH-TOKEN"));
        assertFalse(service.tokenMatches("other"));
        assertFalse(service.tokenMatches(null));
    }

    @Test
    void blankConfiguredTokenRejectsEverything() {
        var s = new AsaasWebhookService(payments, events, new BillingProperties(true, "u", "KEY", ""));
        assertFalse(s.tokenMatches(""));
    }

    @Test
    void paymentEventIsAppliedAndMarked() throws Exception {
        when(events.register("evt_1", "PAYMENT_RECEIVED")).thenReturn(false);
        when(payments.apply("pay_1")).thenReturn(Optional.of(UUID.randomUUID()));
        service.handle(body("PAYMENT_RECEIVED"));
        verify(payments).apply("pay_1");
        verify(events).markProcessed("evt_1");
    }

    @Test
    void repeatedEventIsIgnored() throws Exception {
        when(events.register("evt_1", "PAYMENT_RECEIVED")).thenReturn(true);
        service.handle(body("PAYMENT_RECEIVED"));
        verifyNoInteractions(payments);
    }

    @Test
    void otherEventsAreRecordedAndIgnored() throws Exception {
        when(events.register("evt_1", "PAYMENT_UPDATED")).thenReturn(false);
        service.handle(body("PAYMENT_UPDATED"));
        verifyNoInteractions(payments);
        verify(events).markProcessed("evt_1");
    }

    @Test
    void asaasFailureLeavesEventUnprocessedWithoutThrowing() throws Exception {
        when(events.register("evt_1", "PAYMENT_OVERDUE")).thenReturn(false);
        when(payments.apply("pay_1")).thenThrow(new AsaasClient.AsaasException("Asaas indisponível."));
        assertDoesNotThrow(() -> service.handle(body("PAYMENT_OVERDUE")));
        verify(events, never()).markProcessed(any());
    }
}
