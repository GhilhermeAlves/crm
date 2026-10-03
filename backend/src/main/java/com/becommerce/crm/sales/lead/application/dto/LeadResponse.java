package com.becommerce.crm.sales.lead.application.dto;

import com.becommerce.crm.sales.lead.domain.LeadClassification;
import com.becommerce.crm.sales.lead.domain.LeadSource;
import com.becommerce.crm.sales.lead.domain.LeadStatus;

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