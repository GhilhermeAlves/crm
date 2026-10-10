package com.becommerce.crm.finance.transaction.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Payload de pagamento: {@code { "paidAt": "yyyy-MM-dd" }}. */
public record PayTransactionRequest(@NotNull LocalDate paidAt) {
}
