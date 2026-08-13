package com.becommerce.crm.application.lead.dto;

import com.becommerce.crm.domain.lead.LeadClassification;
import com.becommerce.crm.domain.lead.LeadSource;
import com.becommerce.crm.domain.lead.LeadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        UUID companyId,
        UUID contactId,
        LeadStatus status,
        int score,
        LeadClassification classification,
        LeadSource source,
        UUID campaignId,
        UUID assignedTo,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}