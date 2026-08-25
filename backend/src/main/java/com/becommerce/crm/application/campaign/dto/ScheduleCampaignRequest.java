package com.becommerce.crm.application.campaign.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleCampaignRequest(
        @Future LocalDateTime scheduledAt,
        @Size(max = 50) String timezone
) {
}
