package com.becommerce.crm.sales.holiday.application.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateHolidayRequest(
        LocalDate date,
        @Size(max = 120, message = "Nome do feriado deve ter no máximo 120 caracteres")
        String name,
        Boolean recurringYearly,
        Boolean blocksSchedule
) {}
