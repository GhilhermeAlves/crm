package com.becommerce.crm.application.followup.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de execução de follow-up (Sprint 23). Publicado pelo scheduler após o
 * claim atômico (PENDING -> PROCESSING) e consumido pela fila
 * {@code crm.followup.executor}.
 *
 * <p>O claim atômico continua no scheduler (idempotência de execução entre
 * instâncias); a validação pesada (conversa, HUMAN, staleness) e a preparação
 * do envio são adiadas para o consumer.
 */
public record FollowUpExecutionEvent(
        UUID eventId,
        UUID companyId,
        UUID followUpId,
        LocalDateTime occurredAt
) {

    public static FollowUpExecutionEvent of(UUID companyId, UUID followUpId) {
        return new FollowUpExecutionEvent(UUID.randomUUID(), companyId, followUpId, LocalDateTime.now());
    }
}