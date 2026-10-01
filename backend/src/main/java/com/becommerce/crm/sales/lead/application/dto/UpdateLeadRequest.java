package com.becommerce.crm.sales.lead.application.dto;

import com.becommerce.crm.sales.lead.domain.LeadClassification;
import com.becommerce.crm.sales.lead.domain.LeadStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateLeadRequest(
        LeadStatus status,
        @Min(value = 0, message = "score deve ser entre 0 e 100.")
        @Max(value = 100, message = "score deve ser entre 0 e 100.")
        Integer score,
        LeadClassification classification,
        UUID campaignId,
        UUID assignedTo,
        @Size(max = 1000, message = "notas devem ter no máximo 1000 caracteres.")
        String notes
) {}