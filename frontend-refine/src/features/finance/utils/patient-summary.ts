import { isBefore, parseISO, startOfDay } from "date-fns";

import type { FinancialTransaction } from "../types/finance.types";

export type PatientFinanceSummary = {
  totalCents: number;
  paidCents: number;
  pendingCents: number;
  overdueCents: number;
};

/**
 * Resume os lançamentos de receita de um paciente em centavos.
 *
 * - `totalCents`: tudo que não foi cancelado;
 * - `paidCents`: apenas os pagos;
 * - `pendingCents`: pendentes com vencimento hoje ou no futuro;
 * - `overdueCents`: pendentes com vencimento anterior a hoje.
 *
 * `today` é injetável para permitir testes na virada do dia.
 */
export function summarizePatientTransactions(
  transactions: FinancialTransaction[],
  today: Date = new Date(),
): PatientFinanceSummary {
  const reference = startOfDay(today);
  const summary: PatientFinanceSummary = {
    totalCents: 0,
    paidCents: 0,
    pendingCents: 0,
    overdueCents: 0,
  };

  for (const transaction of transactions) {
    if (transaction.status === "CANCELLED") continue;

    summary.totalCents += transaction.amountCents;

    if (transaction.status === "PAID") {
      summary.paidCents += transaction.amountCents;
      continue;
    }

    if (transaction.status === "PENDING") {
      if (isBefore(parseISO(transaction.dueDate), reference)) {
        summary.overdueCents += transaction.amountCents;
      } else {
        summary.pendingCents += transaction.amountCents;
      }
    }
  }

  return summary;
}
