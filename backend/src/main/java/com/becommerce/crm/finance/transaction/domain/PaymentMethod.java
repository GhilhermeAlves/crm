package com.becommerce.crm.finance.transaction.domain;

/**
 * Forma de pagamento de um lançamento. Persistida como {@code String}
 * (coluna {@code payment_method}) e validada pelo CHECK da V092.
 */
public enum PaymentMethod {
    BOLETO,
    CREDIT_CARD,
    DEBIT_CARD,
    PIX,
    CASH,
    TRANSFER,
    CHECK,
    OTHER
}
