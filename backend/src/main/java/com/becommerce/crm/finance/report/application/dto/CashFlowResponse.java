package com.becommerce.crm.finance.report.application.dto;

import java.util.List;

/**
 * Fluxo de caixa do período (valores em centavos):
 * <ul>
 *   <li>series: incomeCents/expenseCents = PAID por paid_at; open* = PENDING com
 *       vencimento atual/futuro (>= hoje); periodBalance = receitas - despesas;
 *       accumulatedBalance = saldo acumulado;</li>
 *   <li>incomeByMethod: receitas PAID do período por forma de pagamento;</li>
 *   <li>incomeHistory: forecast = tudo (PAID+PENDING) com vencimento no período;
 *       income = PAID por paid_at; default = PENDING com vencimento passado;
 *       open = PENDING com vencimento hoje/futuro;</li>
 *   <li>expenseHistory: expenseCents = PAID por paid_at; open/default como acima;</li>
 *   <li>currentMonth: caixa do mês corrente (paid/open/default).</li>
 * </ul>
 */
public record CashFlowResponse(
        List<CashFlowSeriesItem> series,
        IncomeByMethod incomeByMethod,
        List<CashFlowHistoryItem> incomeHistory,
        List<ExpenseHistoryItem> expenseHistory,
        CurrentMonth currentMonth) {

    public record CashFlowSeriesItem(String period, long incomeCents, long openReceivablesCents,
                                     long expenseCents, long openPayablesCents,
                                     long periodBalanceCents, long accumulatedBalanceCents) {}

    public record IncomeByMethod(long boletoCents, long creditCardCents, long debitCardCents,
                                 long pixCents, long cashCents, long transferCents,
                                 long checkCents, long otherCents) {}

    public record CashFlowHistoryItem(String period, long forecastCents, long incomeCents,
                                      long defaultCents, long openCents) {}

    public record ExpenseHistoryItem(String period, long expenseCents, long openCents,
                                     long defaultCents) {}

    public record CurrentMonth(MonthAmounts income, MonthAmounts expense) {}

    public record MonthAmounts(long paidCents, long openCents, long defaultCents) {}
}