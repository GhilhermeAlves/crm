package com.becommerce.crm.sales.scheduling.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AgendaSettingsDto(
        @Min(5) @Max(480) int defaultDurationMinutes,
        @NotNull LocalTime workHoursStart,
        @NotNull LocalTime workHoursEnd,
        boolean notifyOnBooking,
        boolean notify24hBefore,
        boolean notify1hBefore,
        boolean notifyOnCancel,
        boolean notifyOnReschedule
) {
    /** Valores usados enquanto a empresa não salvou ajustes (iguais aos defaults da V099). */
    public static AgendaSettingsDto defaults() {
        return new AgendaSettingsDto(30, LocalTime.of(8, 0), LocalTime.of(18, 0),
                true, true, false, true, true);
    }

    @AssertTrue(message = "O início do horário de trabalho deve ser antes do fim.")
    public boolean isWorkHoursValid() {
        return workHoursStart == null || workHoursEnd == null || workHoursStart.isBefore(workHoursEnd);
    }
}
