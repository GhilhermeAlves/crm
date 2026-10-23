package com.becommerce.crm.sales.scheduling.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Regra de repetição de um agendamento/bloqueio; nulo na requisição = não repete. */
public record RecurrenceRequest(
        @NotNull(message = "Frequência da recorrência é obrigatória")
        RecurrenceFrequency frequency,
        @Min(value = 1, message = "O intervalo deve ser entre 1 e 99")
        @Max(value = 99, message = "O intervalo deve ser entre 1 e 99")
        int interval,
        @NotNull(message = "Término da recorrência é obrigatório")
        RecurrenceEnd endType,
        LocalDate until,
        @Min(value = 2, message = "O número de ocorrências deve ser entre 2 e 366")
        @Max(value = 366, message = "O número de ocorrências deve ser entre 2 e 366")
        Integer count
) {}
