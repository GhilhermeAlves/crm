package com.becommerce.crm.automation.campaign.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateCampaignRequest(
        @Size(max = 120) String name,
        @Size(max = 2000) String description,
        /** Opcional: quando informado, troca o público (critérios juntos). */
        String audienceType,
        String audienceCriteria
) {
}
