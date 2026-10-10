package com.becommerce.crm.finance.transaction.domain;

/**
 * Natureza de um lançamento financeiro. Persistido como {@code String}
 * (coluna {@code type}) e validado pelo CHECK da V092.
 */
public enum TransactionType {
    INCOME,
    EXPENSE
}
