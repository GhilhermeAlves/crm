package com.becommerce.crm.sales.scheduling.application.dto;

import java.time.LocalTime;

/**
 * Faixa semanal de disponibilidade. {@code weekday} é o número ISO
 * (1 = segunda … 7 = domingo) — o mesmo que o frontend e a coluna do banco usam.
 * Não usar {@code DayOfWeek} aqui: o Jackson lê número como ÍNDICE do enum
 * (0 = segunda), o que deslocava todos os dias em um.
 */
public record AvailabilityRuleDto(
        int weekday,
        LocalTime startTime,
        LocalTime endTime
) {}
