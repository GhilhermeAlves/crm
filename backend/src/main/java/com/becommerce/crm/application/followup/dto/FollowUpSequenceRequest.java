package com.becommerce.crm.application.followup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de criação/edição de {@code FollowUpSequence} (Sprint 22).
 * O companyId vem do usuário autenticado, nunca do body.
 */
public record FollowUpSequenceRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 4000) String description
) {
}