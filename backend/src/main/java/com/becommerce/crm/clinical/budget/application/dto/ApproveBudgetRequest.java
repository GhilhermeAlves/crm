package com.becommerce.crm.clinical.budget.application.dto;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ApproveBudgetRequest(@NotNull PaymentMethod paymentMethod, @Min(1) @Max(24) int installments,
                                   @NotNull LocalDate firstDueDate) {}
