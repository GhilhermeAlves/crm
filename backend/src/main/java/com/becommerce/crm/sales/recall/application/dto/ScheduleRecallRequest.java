package com.becommerce.crm.sales.recall.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ScheduleRecallRequest(
        @NotNull(message = "Agendamento é obrigatório")
        UUID appointmentId
) {}
