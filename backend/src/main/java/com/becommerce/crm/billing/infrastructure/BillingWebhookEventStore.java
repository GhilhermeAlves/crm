package com.becommerce.crm.billing.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/** Idempotência do webhook (billing_webhook_events, sem RLS: tabela de plataforma). */
@Component
public class BillingWebhookEventStore {

    private final JdbcTemplate jdbc;

    public BillingWebhookEventStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Registra o evento. Devolve true se ele já tinha sido processado (nada a fazer). */
    public boolean register(String eventId, String event) {
        int inserted = jdbc.update(
                "INSERT INTO billing_webhook_events (asaas_event_id, event) VALUES (?, ?) "
                        + "ON CONFLICT (asaas_event_id) DO NOTHING", eventId, truncate(event, 60));
        if (inserted == 1) return false;
        List<Object> processed = jdbc.queryForList(
                "SELECT processed_at FROM billing_webhook_events WHERE asaas_event_id = ?", Object.class, eventId);
        return !processed.isEmpty() && processed.get(0) != null;
    }

    public void markProcessed(String eventId) {
        jdbc.update("UPDATE billing_webhook_events SET processed_at = now() WHERE asaas_event_id = ?", eventId);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "UNKNOWN";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
