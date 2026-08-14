package com.becommerce.crm.application.customer360.dto;

/**
 * Próxima ação recomendada, determinística (sem IA), seguindo a mesma lógica de
 * priorização do dashboard operacional.
 *
 * <p>{@code type}: {@code FOLLOW_UP}, {@code COMPLETE_TASK},
 * {@code REVIEW_CLOSING}, {@code FORMAL_PROPOSAL}, {@code NONE}.
 */
public record NextActionResponse(
        String type,
        String title,
        String description,
        int priority
) {}