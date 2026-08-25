package com.becommerce.crm.application.campaign.dto;

import jakarta.validation.constraints.Size;

public record UpdateCampaignRequest(
        @Size(max = 120) String name,
        @Size(max = 2000) String description
) {
}
