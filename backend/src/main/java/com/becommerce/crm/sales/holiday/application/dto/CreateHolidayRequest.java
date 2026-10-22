package com.becommerce.crm.sales.holiday.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateHolidayRequest(
        @NotNull(message = "Data é obrigatória")
        LocalDate date,
        @NotBlank(message = "Nome do feriado é obrigatório")
        @Size(max = 120, message = "Nome do feriado deve ter no máximo 120 caracteres")
        String name,
        Boolean recurringYearly,
        Boolean blocksSchedule
) {}
