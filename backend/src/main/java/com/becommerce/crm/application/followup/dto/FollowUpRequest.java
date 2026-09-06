package com.becommerce.crm.application.followup.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Requisição de agendamento de FollowUp (Sprint 22). {@code idempotencyKey}
 * opcional: se reenviada com sucesso na mesma empresa, o serviço devolve o
 * follow-up existente em vez de criar duplicado.
 */
public record FollowUpRequest(
        UUID conversationId,
        @NotBlank String content,
        LocalDateTime executeAt,
        UUID idempotencyKey
) {
}