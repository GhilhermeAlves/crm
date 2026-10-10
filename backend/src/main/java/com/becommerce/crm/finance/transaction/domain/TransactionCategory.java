package com.becommerce.crm.finance.transaction.domain;

/**
 * Categoria de um lançamento, sempre vinculada a um {@link TransactionType}.
 * Persistida como {@code String} (coluna {@code category}) e validada pelo
 * CHECK da V092. A compatibilidade categoria x tipo é verificada no domínio.
 */
public enum TransactionCategory {

    PROCEDURE(TransactionType.INCOME),
    CONSULTATION(TransactionType.INCOME),
    PRODUCT_SALE(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME),

    RENT(TransactionType.EXPENSE),
    PAYROLL(TransactionType.EXPENSE),
    SUPPLIES(TransactionType.EXPENSE),
    LAB(TransactionType.EXPENSE),
    TAXES(TransactionType.EXPENSE),
    MARKETING(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE),
    EQUIPMENT(TransactionType.EXPENSE),
    OTHER_EXPENSE(TransactionType.EXPENSE);

    private final TransactionType type;

    TransactionCategory(TransactionType type) {
        this.type = type;
    }

    /** Tipo (receita/despesa) a que esta categoria pertence. */
    public TransactionType type() {
        return type;
    }
}
