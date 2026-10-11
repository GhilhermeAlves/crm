package com.becommerce.crm.finance.transaction.application.dto;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Edição de um lançamento existente (não altera o parcelamento). */
public record UpdateFinancialTransactionRequest(
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
        LocalDate paidAt
) {
}
