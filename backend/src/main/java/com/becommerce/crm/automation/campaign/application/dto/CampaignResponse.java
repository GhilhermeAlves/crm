package com.becommerce.crm.automation.campaign.application.dto;

import com.becommerce.crm.automation.campaign.domain.AudienceType;
import com.becommerce.crm.automation.campaign.domain.CampaignStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        CampaignStatus status,
        AudienceType audienceType,
        String audienceCriteria,
        int estimatedRecipients,
        LocalDateTime scheduledAt,
        String timezone,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        UUID createdBy,
        UUID channelId,
        String channelType,
        UUID providerChannelId,
        UUID templateId,
        Integer templateVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
