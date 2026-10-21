package com.becommerce.crm.sales.recall.domain;

import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;

import java.time.LocalDate;

/**
 * Janela de vencimento dos retornos programados. Os limites são semiabertos:
 * {@code [fromInclusive, toExclusive)}.
 */
public enum RecallWindow {
    TODAY,
    NEXT_7_DAYS,
    NEXT_30_DAYS,
    OVERDUE,
    ALL;

    public LocalDate fromInclusive(LocalDate today) {
        return switch (this) {
            case OVERDUE, ALL -> null;
            case TODAY, NEXT_7_DAYS, NEXT_30_DAYS -> today;
        };
    }

    public LocalDate toExclusive(LocalDate today) {
        return switch (this) {
            case TODAY -> today.plusDays(1);
            case NEXT_7_DAYS -> today.plusDays(8);
            case NEXT_30_DAYS -> today.plusDays(31);
            case OVERDUE -> today;
            case ALL -> null;
        };
    }

    public static RecallWindow from(String value) {
        if (value == null || value.isBlank()) {
            return NEXT_7_DAYS;
        }
        try {
            return RecallWindow.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RecallValidationException("Janela de retorno inválida: " + value);
        }
    }
}
