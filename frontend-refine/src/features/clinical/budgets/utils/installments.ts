import { addMonths, format, parse } from "date-fns";
import type { BudgetStatus } from "../types/budget.types";

export const BUDGET_STATUS_LABELS: Record<BudgetStatus, string> = {
  DRAFT: "Rascunho",
  APPROVED: "Aprovado",
  LOST: "Perdido",
};

export type InstallmentPreview = { dueDate: string; amountCents: number };

/** Divide o total em parcelas iguais (resto na 1ª) com vencimentos mensais. */
export function splitInstallments(
  totalCents: number,
  count: number,
  firstDueDate: string,
): InstallmentPreview[] {
  const base = Math.floor(totalCents / count);
  const remainder = totalCents - base * count;
  const first = parse(firstDueDate, "yyyy-MM-dd", new Date());
  return Array.from({ length: count }, (_, i) => ({
    dueDate: format(addMonths(first, i), "yyyy-MM-dd"),
    amountCents: i === 0 ? base + remainder : base,
  }));
}
