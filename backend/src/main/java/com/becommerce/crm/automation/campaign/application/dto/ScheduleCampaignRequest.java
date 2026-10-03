package com.becommerce.crm.automation.campaign.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleCampaignRequest(
        @Future LocalDateTime scheduledAt,
        @Size(max = 50) String timezone
) {
}
