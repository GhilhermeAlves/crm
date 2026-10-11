package com.becommerce.crm.finance.transaction.application.dto;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Criação de um lançamento. Quando {@code installments > 1}, a API gera N
 * lançamentos parcelados com vencimentos mensais a partir de {@code dueDate}.
 */
public record CreateFinancialTransactionRequest(
        @NotNull TransactionType type,
        @NotBlank @Size(max = 255) String description,
        @NotNull TransactionCategory category,
        @NotNull @Positive Long amountCents,
        @PositiveOrZero Long feeCents,
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate dueDate,
        UUID contactId,
        UUID professionalUserId,
        UUID catalogItemId,
        @Size(max = 4000) String notes,
        LocalDate paidAt,
        @Min(1) @Max(24) Integer installments
) {
}
