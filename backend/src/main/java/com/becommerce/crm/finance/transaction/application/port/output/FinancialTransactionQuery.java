package com.becommerce.crm.finance.transaction.application.port.output;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros da listagem paginada de lançamentos. Campos nulos não filtram.
 *
 * <p>{@code overdueBefore} implementa o status calculado "OVERDUE": quando
 * preenchido, restringe a lançamentos {@code PENDING} com {@code dueDate}
 * anterior à data informada (hoje). "Atrasado" não é um status gravado.
 */
public record FinancialTransactionQuery(
        TransactionType type,
        String status,
        LocalDate overdueBefore,
        LocalDate from,
        LocalDate to,
        PaymentMethod paymentMethod,
        TransactionCategory category,
        UUID professionalUserId,
        UUID contactId,
        String pattern
) {
}
