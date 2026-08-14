package com.becommerce.crm.application.customer360.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Linha da linha do tempo unificada do contato.
 *
 * <p>{@code type} é um dos marcadores: {@code ACTIVITY}, {@code TASK_CREATED},
 * {@code TASK_COMPLETED}, {@code OPPORTUNITY_CREATED}, {@code OPPORTUNITY_MOVED},
 * {@code OPPORTUNITY_WON}, {@code OPPORTUNITY_LOST}.
 */
public record TimelineEventResponse(
        UUID id,
        String type,
        String title,
        String description,
        LocalDateTime occurredAt,
        UUID referenceId,
        String subject
) {}