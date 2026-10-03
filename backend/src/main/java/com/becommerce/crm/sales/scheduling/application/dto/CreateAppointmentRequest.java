package com.becommerce.crm.sales.scheduling.application.dto;

import com.becommerce.crm.sales.scheduling.domain.LocationKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
        UUID appointmentTypeId,
        @NotNull(message = "Responsável é obrigatório")
        UUID hostId,
        UUID contactId,
        UUID opportunityId,
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String title,
        @NotNull(message = "Data de início é obrigatória")
        Instant startAt,
        @NotNull(message = "Data de fim é obrigatória")
        Instant endAt,
        LocationKind locationKind,
        String locationDetail,
        String notes,
        Boolean force
) {}
