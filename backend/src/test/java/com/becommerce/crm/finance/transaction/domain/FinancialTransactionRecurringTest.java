package com.becommerce.crm.finance.transaction.domain;

import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialTransactionRecurringTest {

    @Test
    void repeatsFullAmountMonthlyAndOnlyFirstIsPaid() {
        LocalDate due = LocalDate.of(2026, 1, 10);
        List<FinancialTransaction> series = FinancialTransaction.createRecurring(UUID.randomUUID(),
                TransactionType.EXPENSE, "Aluguel", TransactionCategory.RENT, 250_000L, 0L,
                PaymentMethod.PIX, due, null, null, null, null, due, 3, UUID.randomUUID());

        assertThat(series).hasSize(3);
        assertThat(series).allMatch(t -> t.getAmountCents() == 250_000L);
        assertThat(series).extracting(FinancialTransaction::getDueDate)
                .containsExactly(due, due.plusMonths(1), due.plusMonths(2));
        assertThat(series.get(0).getStatus()).isEqualTo(TransactionStatus.PAID);
        assertThat(series.subList(1, 3)).allMatch(t -> t.getStatus() == TransactionStatus.PENDING);
        assertThat(series).extracting(FinancialTransaction::getInstallmentGroupId).containsOnly(
                series.get(0).getInstallmentGroupId());
    }

    @Test
    void rejectsSingleMonth() {
        assertThatThrownBy(() -> FinancialTransaction.createRecurring(UUID.randomUUID(),
                TransactionType.EXPENSE, "Aluguel", TransactionCategory.RENT, 1000L, 0L, PaymentMethod.PIX,
                LocalDate.of(2026, 1, 10), null, null, null, null, null, 1, UUID.randomUUID()))
                .isInstanceOf(FinancialTransactionValidationException.class);
    }
}
