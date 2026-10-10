package com.becommerce.crm.finance.accounting.application.dto;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.LocalDate;

/**
 * Lançamento já validado proveniente de uma linha do CSV (Etapa 3). Ainda não
 * tem resolução de paciente/profissional (feita com o banco) nem id persistido.
 */
public record CsvImportRow(
        int line,
        TransactionType type,
        String description,
        TransactionCategory category,
        long amountCents,
        PaymentMethod paymentMethod,
        LocalDate dueDate,
        LocalDate paidAt,
        String patientName,
        String professional,
        String notes
) {
}