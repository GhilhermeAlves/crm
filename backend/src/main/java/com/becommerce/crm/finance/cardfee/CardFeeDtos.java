package com.becommerce.crm.finance.cardfee;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTOs e validação das taxas do cartão (clínica e modelos). */
public final class CardFeeDtos {

    public static final int MAX_INSTALLMENTS = 12;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private CardFeeDtos() {
    }

    public record RateItem(Integer installments, BigDecimal ratePercent) {
    }

    public record SaveRatesRequest(List<RateItem> rates) {
    }

    public record PresetRequest(String name, Boolean estimated, LocalDate checkedAt,
                                String sourceNote, Boolean active, List<RateItem> rates) {
    }

    public record PresetResponse(UUID id, String name, boolean estimated, LocalDate checkedAt,
                                 String sourceNote, boolean active, List<RateItem> rates) {
    }

    /**
     * Exige as 12 parcelas (1 a 12, sem repetir) com taxa entre 0 e 100 (exclusive) e no
     * máximo 2 casas. Devolve a lista ordenada por parcelas.
     */
    public static List<RateItem> validate(List<RateItem> rates) {
        if (rates == null || rates.size() != MAX_INSTALLMENTS) {
            throw bad("Informe as taxas de 1x a 12x.");
        }
        Set<Integer> seen = new HashSet<>();
        for (RateItem item : rates) {
            if (item == null || item.installments() == null
                    || item.installments() < 1 || item.installments() > MAX_INSTALLMENTS) {
                throw bad("Parcelas devem ir de 1 a 12.");
            }
            if (!seen.add(item.installments())) {
                throw bad("Parcela " + item.installments() + "x repetida.");
            }
            BigDecimal rate = item.ratePercent();
            if (rate == null || rate.signum() < 0 || rate.compareTo(HUNDRED) >= 0) {
                throw bad("A taxa de " + item.installments() + "x deve ficar entre 0% e 99,99%.");
            }
            if (rate.stripTrailingZeros().scale() > 2) {
                throw bad("Use no máximo 2 casas decimais na taxa de " + item.installments() + "x.");
            }
        }
        return rates.stream()
                .sorted((a, b) -> Integer.compare(a.installments(), b.installments()))
                .toList();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
