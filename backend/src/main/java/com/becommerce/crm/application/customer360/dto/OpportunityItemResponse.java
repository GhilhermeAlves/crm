package com.becommerce.crm.application.customer360.dto;

import com.becommerce.crm.domain.pipeline.OpportunityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Oportunidade no contexto do contato (Customer 360). */
public record OpportunityItemResponse(
        UUID id,
        String title,
        BigDecimal value,
        String stageName,
        int probability,
        OpportunityStatus status,
        String statusLabel,
        String pipelineName,
        UUID assignedTo,
        LocalDateTime expectedCloseDate
) {}