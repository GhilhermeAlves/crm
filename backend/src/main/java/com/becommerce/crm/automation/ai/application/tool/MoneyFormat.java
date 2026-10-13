package com.becommerce.crm.automation.ai.application.tool;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formata valores monetarios (em centavos) para o padrao pt-BR usado nas
 * respostas do assistente: {@code R$ 1.234,56}. {@link NumberFormat} nao e
 * thread-safe, por isso um formatador e criado a cada chamada.
 */
public final class MoneyFormat {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private MoneyFormat() {
    }

    public static String brl(long cents) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "R$ " + format.format(BigDecimal.valueOf(cents, 2));
    }
}
