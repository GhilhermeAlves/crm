package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceEnd;
import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceRequest;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Expande uma regra de recorrência em intervalos concretos (o primeiro é o original).
 * As ocorrências são calculadas na data local de America/Sao_Paulo mantendo o horário
 * e a duração (DST-safe); MONTHLY/YEARLY partem sempre do original (dia 31 → último
 * dia do mês). NEVER gera até 1 ano após o início; teto absoluto de 366 ocorrências.
 */
public final class RecurrenceExpander {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    public static final int MAX_OCCURRENCES = 366;

    private RecurrenceExpander() {}

    public static List<Interval> expand(Instant start, Instant end, RecurrenceRequest recurrence) {
        List<Interval> result = new ArrayList<>();
        result.add(new Interval(start, end));
        if (recurrence == null) {
            return result;
        }
        validate(recurrence);
        Duration duration = Duration.between(start, end);
        LocalDateTime origin = LocalDateTime.ofInstant(start, ZONE);
        LocalDate originDate = origin.toLocalDate();

        int limit;
        LocalDate lastDate;
        switch (recurrence.endType()) {
            case AFTER_COUNT -> {
                limit = recurrence.count();
                lastDate = null;
            }
            case ON_DATE -> {
                if (recurrence.until().isBefore(originDate)) {
                    throw new SchedulingValidationException(
                            "A data de término da recorrência não pode ser anterior à data inicial.");
                }
                limit = MAX_OCCURRENCES;
                lastDate = recurrence.until();
            }
            default -> {
                limit = MAX_OCCURRENCES;
                lastDate = originDate.plusYears(1);
            }
        }

        for (int k = 1; result.size() < limit; k++) {
            LocalDateTime next = step(origin, recurrence, k);
            if (lastDate != null && next.toLocalDate().isAfter(lastDate)) {
                break;
            }
            Instant s = next.atZone(ZONE).toInstant();
            result.add(new Interval(s, s.plus(duration)));
        }
        return result;
    }

    private static LocalDateTime step(LocalDateTime origin, RecurrenceRequest r, int k) {
        long n = (long) r.interval() * k;
        return switch (r.frequency()) {
            case DAILY -> origin.plusDays(n);
            case WEEKLY -> origin.plusWeeks(n);
            case MONTHLY -> origin.plusMonths(n);
            case YEARLY -> origin.plusYears(n);
        };
    }

    private static void validate(RecurrenceRequest r) {
        if (r.frequency() == null) {
            throw new SchedulingValidationException("Frequência da recorrência é obrigatória.");
        }
        if (r.interval() < 1 || r.interval() > 99) {
            throw new SchedulingValidationException("O intervalo da recorrência deve ser entre 1 e 99.");
        }
        if (r.endType() == null) {
            throw new SchedulingValidationException("Término da recorrência é obrigatório.");
        }
        if (r.endType() == RecurrenceEnd.ON_DATE && r.until() == null) {
            throw new SchedulingValidationException("Informe a data de término da recorrência.");
        }
        if (r.endType() == RecurrenceEnd.AFTER_COUNT
                && (r.count() == null || r.count() < 2 || r.count() > MAX_OCCURRENCES)) {
            throw new SchedulingValidationException(
                    "O número de ocorrências deve ser entre 2 e " + MAX_OCCURRENCES + ".");
        }
    }
}
