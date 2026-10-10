package com.becommerce.crm.finance.transaction.domain;

/**
 * Status persistido de um lançamento. "Atrasado" NÃO é um status gravado: é
 * {@link #PENDING} com {@code dueDate} anterior a hoje, calculado na leitura
 * (ver {@link FinancialTransaction#isOverdue(java.time.LocalDate)}).
 */
public enum TransactionStatus {
    PENDING,
    PAID,
    CANCELLED
}
