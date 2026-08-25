package com.becommerce.crm.application.campaign.dto;

import com.becommerce.crm.domain.campaign.AudienceType;
import com.becommerce.crm.domain.campaign.CampaignStatus;

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
