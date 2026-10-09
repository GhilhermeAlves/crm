package com.becommerce.crm.shared.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Feriados nacionais do Brasil — mesma lista do calendário do frontend
 * ({@code src/lib/holidays.ts}), com Páscoa calculada (algoritmo de Meeus).
 * Usada pela agenda (dia de feriado não tem horário livre) e pelo agente.
 */
public final class BrazilianHolidays {

    public record Holiday(LocalDate date, String name) {
    }

    private BrazilianHolidays() {
    }

    public static List<Holiday> ofYear(int year) {
        LocalDate easter = easterSunday(year);
        return List.of(
                new Holiday(LocalDate.of(year, 1, 1), "Confraternização Universal"),
                new Holiday(easter.minusDays(48), "Carnaval"),
                new Holiday(easter.minusDays(47), "Carnaval"),
                new Holiday(easter.minusDays(2), "Sexta-feira Santa"),
                new Holiday(easter, "Páscoa"),
                new Holiday(LocalDate.of(year, 4, 21), "Tiradentes"),
                new Holiday(LocalDate.of(year, 5, 1), "Dia do Trabalho"),
                new Holiday(easter.plusDays(60), "Corpus Christi"),
                new Holiday(LocalDate.of(year, 9, 7), "Independência do Brasil"),
                new Holiday(LocalDate.of(year, 10, 12), "Nossa Sra. Aparecida"),
                new Holiday(LocalDate.of(year, 11, 2), "Finados"),
                new Holiday(LocalDate.of(year, 11, 15), "Proclamação da República"),
                new Holiday(LocalDate.of(year, 11, 20), "Dia da Consciência Negra"),
                new Holiday(LocalDate.of(year, 12, 25), "Natal"));
    }

    public static Optional<Holiday> on(LocalDate date) {
        return ofYear(date.getYear()).stream().filter(h -> h.date().equals(date)).findFirst();
    }

    public static boolean isHoliday(LocalDate date) {
        return on(date).isPresent();
    }

    /** Feriados entre {@code from} e {@code to} (inclusive), em ordem. */
    public static List<Holiday> between(LocalDate from, LocalDate to) {
        List<Holiday> result = new ArrayList<>();
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            for (Holiday h : ofYear(year)) {
                if (!h.date().isBefore(from) && !h.date().isAfter(to)) {
                    result.add(h);
                }
            }
        }
        result.sort(java.util.Comparator.comparing(Holiday::date));
        return result;
    }

    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
