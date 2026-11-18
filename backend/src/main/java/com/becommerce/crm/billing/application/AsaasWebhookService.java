package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.infrastructure.BillingWebhookEventStore;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Service
public class AsaasWebhookService {

    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookService.class);
    static final Set<String> HANDLED = Set.of("PAYMENT_CREATED", "PAYMENT_CONFIRMED", "PAYMENT_RECEIVED",
            "PAYMENT_OVERDUE", "PAYMENT_REFUNDED", "PAYMENT_DELETED");

    private final BillingPaymentService payments;
    private final BillingWebhookEventStore events;
    private final BillingProperties props;

    public AsaasWebhookService(BillingPaymentService payments, BillingWebhookEventStore events,
                               BillingProperties props) {
        this.payments = payments;
        this.events = events;
        this.props = props;
    }

    public boolean tokenMatches(String header) {
        String expected = props.webhookToken();
        if (expected == null || expected.isBlank() || header == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                header.getBytes(StandardCharsets.UTF_8));
    }

    public void handle(JsonNode body) {
        String event = body.path("event").asText("UNKNOWN");
        String paymentId = body.path("payment").path("id").asText("");
        String eventId = body.path("id").asText("");
        if (eventId.isBlank()) eventId = event + ":" + paymentId;

        if (events.register(eventId, event, paymentId)) return;
        if (!HANDLED.contains(event) || paymentId.isBlank()) {
            events.markProcessed(eventId);
            return;
        }
        try {
            payments.apply(paymentId);
            events.markProcessed(eventId);
        } catch (RuntimeException e) {
            log.warn("[BILLING] webhook {} ({}) não aplicado: {}", eventId, event, e.getMessage());
        }
    }

    /** Reaplica eventos que falharam (o Asaas já recebeu 200 e não reenvia). Chamado pelo job diário. */
    public int reprocessPending() {
        int done = 0;
        for (BillingWebhookEventStore.Pending p : events.pending(200)) {
            try {
                payments.apply(p.paymentId());
                events.markProcessed(p.eventId());
                done++;
            } catch (RuntimeException e) {
                log.warn("[BILLING] reprocesso do webhook {} falhou: {}", p.eventId(), e.getMessage());
            }
        }
        return done;
    }
}
