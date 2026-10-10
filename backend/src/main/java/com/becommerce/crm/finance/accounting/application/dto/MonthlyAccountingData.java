package com.becommerce.crm.finance.accounting.application.dto;

import com.becommerce.crm.finance.transaction.domain.TransactionCategory;

import java.util.List;

/**
 * Dados montados do "Relatório Contábil Mensal" (Etapa 3): receitas e despesas
 * PAID do mês somadas por categoria (rótulo pt-BR), totais, taxas e resultado.
 */
public record MonthlyAccountingData(
        List<MonthlyCategory> incomeCategories,
        List<MonthlyCategory> expenseCategories,
        long incomeTotalCents,
        long expenseTotalCents,
        long feeTotalCents
) {
    public long resultCents() {
        return incomeTotalCents - expenseTotalCents;
    }

    public record MonthlyCategory(TransactionCategory category, long totalCents) {}
}