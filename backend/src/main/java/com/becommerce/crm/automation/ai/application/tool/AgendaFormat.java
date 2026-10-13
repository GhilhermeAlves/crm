package com.becommerce.crm.automation.ai.application.tool;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação de horários da agenda para o assistente: o modelo lê e escreve
 * horários LOCAIS da clínica (São Paulo); a conversão para {@link Instant}
 * fica no backend.
 */
public final class AgendaFormat {

    public static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter SHORT =
            DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

    private AgendaFormat() {
    }

    public static String dateTime(Instant instant) {
        return instant == null ? null : DATE_TIME.format(instant.atZone(CLINIC_ZONE));
    }

    public static String shortDateTime(Instant instant) {
        return instant == null ? null : SHORT.format(instant.atZone(CLINIC_ZONE));
    }

    public static String date(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }
}
