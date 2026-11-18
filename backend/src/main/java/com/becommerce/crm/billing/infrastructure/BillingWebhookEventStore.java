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
    public boolean register(String eventId, String event, String paymentId) {
        int inserted = jdbc.update(
                "INSERT INTO billing_webhook_events (asaas_event_id, event, payment_id) VALUES (?, ?, ?) "
                        + "ON CONFLICT (asaas_event_id) DO NOTHING",
                eventId, truncate(event, 60), paymentId == null || paymentId.isBlank() ? null : truncate(paymentId, 100));
        if (inserted == 1) return false;
        List<Object> processed = jdbc.queryForList(
                "SELECT processed_at FROM billing_webhook_events WHERE asaas_event_id = ?", Object.class, eventId);
        return !processed.isEmpty() && processed.get(0) != null;
    }

    public void markProcessed(String eventId) {
        jdbc.update("UPDATE billing_webhook_events SET processed_at = now() WHERE asaas_event_id = ?", eventId);
    }

    /** Evento ainda não aplicado, com o pagamento a reconsultar. */
    public record Pending(String eventId, String paymentId) {}

    /** Eventos com falha (não processados) recebidos há mais de 5 minutos. */
    public List<Pending> pending(int limit) {
        return jdbc.query(
                "SELECT asaas_event_id, payment_id FROM billing_webhook_events "
                        + "WHERE processed_at IS NULL AND payment_id IS NOT NULL "
                        + "AND received_at < now() - interval '5 minutes' ORDER BY received_at LIMIT ?",
                (rs, i) -> new Pending(rs.getString(1), rs.getString(2)), limit);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "UNKNOWN";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
