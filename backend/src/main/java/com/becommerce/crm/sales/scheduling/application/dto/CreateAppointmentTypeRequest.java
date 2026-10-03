package com.becommerce.crm.sales.scheduling.application.dto;

import com.becommerce.crm.sales.scheduling.domain.AssignmentMode;
import com.becommerce.crm.sales.scheduling.domain.LocationKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateAppointmentTypeRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String name,
        @NotBlank(message = "Slug é obrigatório")
        @Size(max = 80, message = "Slug deve ter no máximo 80 caracteres")
        String slug,
        String description,
        @NotNull(message = "Duração é obrigatória")
        Integer durationMinutes,
        Integer bufferBeforeMinutes,
        Integer bufferAfterMinutes,
        Integer minNoticeHours,
        Integer maxDaysAhead,
        Integer slotIntervalMinutes,
        String color,
        LocationKind locationKind,
        String locationDetail,
        AssignmentMode assignmentMode,
        List<UUID> hostIds
) {}
