package com.becommerce.crm.sales.scheduling.application.dto;

import com.becommerce.crm.sales.scheduling.domain.LocationKind;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateAppointmentRequest(
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String title,
        UUID contactId,
        UUID opportunityId,
        LocationKind locationKind,
        String locationDetail,
        String notes
) {}
