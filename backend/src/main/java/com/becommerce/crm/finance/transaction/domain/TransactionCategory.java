package com.becommerce.crm.finance.transaction.domain;

/**
 * Categoria de um lançamento, sempre vinculada a um {@link TransactionType}.
 * Persistida como {@code String} (coluna {@code category}) e validada pelo
 * CHECK da V121. A compatibilidade categoria x tipo é verificada no domínio.
 */
public enum TransactionCategory {

    // Receitas
    PROCEDURE(TransactionType.INCOME),
    CONSULTATION(TransactionType.INCOME),
    PRODUCT_SALE(TransactionType.INCOME),
    INSURANCE_TRANSFER(TransactionType.INCOME),
    FINANCIAL_INCOME(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME),

    // Impostos e taxas
    TAX_DAS(TransactionType.EXPENSE),
    TAX_ISS(TransactionType.EXPENSE),
    TAX_IRPJ(TransactionType.EXPENSE),
    TAX_CSLL(TransactionType.EXPENSE),
    TAX_INSS(TransactionType.EXPENSE),
    TAX_PIS_COFINS(TransactionType.EXPENSE),
    TAXES(TransactionType.EXPENSE),

    // Financeiras
    BANK_FEES(TransactionType.EXPENSE),
    INTEREST_FINES(TransactionType.EXPENSE),
    CARD_FEES(TransactionType.EXPENSE),
    CREDIT_CHECK(TransactionType.EXPENSE),

    // Pessoal
    PAYROLL(TransactionType.EXPENSE),
    PRO_LABORE(TransactionType.EXPENSE),
    COMMISSION(TransactionType.EXPENSE),
    LABOR_CHARGES(TransactionType.EXPENSE),
    MEAL_VOUCHER(TransactionType.EXPENSE),
    TRANSPORT_VOUCHER(TransactionType.EXPENSE),
    THIRTEENTH_SALARY(TransactionType.EXPENSE),
    VACATION(TransactionType.EXPENSE),

    // Administrativas
    ACCOUNTING(TransactionType.EXPENSE),
    RENT(TransactionType.EXPENSE),
    CONDO_FEE(TransactionType.EXPENSE),
    PROPERTY_TAX(TransactionType.EXPENSE),
    ELECTRICITY(TransactionType.EXPENSE),
    WATER(TransactionType.EXPENSE),
    GAS(TransactionType.EXPENSE),
    INTERNET(TransactionType.EXPENSE),
    POSTAGE(TransactionType.EXPENSE),
    SECURITY(TransactionType.EXPENSE),
    INSURANCE(TransactionType.EXPENSE),
    OFFICE_SUPPLIES(TransactionType.EXPENSE),
    KITCHEN_SUPPLIES(TransactionType.EXPENSE),
    CLEANING_SUPPLIES(TransactionType.EXPENSE),
    EQUIPMENT(TransactionType.EXPENSE),
    PROFESSIONAL_COUNCIL(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE),

    // Divulgação e marketing
    MARKETING(TransactionType.EXPENSE),

    // Despesas clínicas
    SUPPLIES(TransactionType.EXPENSE),
    LAB(TransactionType.EXPENSE),
    PROSTHETIST(TransactionType.EXPENSE),

    // Outras
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
