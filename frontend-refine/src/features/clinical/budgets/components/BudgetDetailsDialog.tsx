"use client";

import Link from "next/link";
import { format, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";
import { centsToBRL } from "@/features/finance/utils/money";
import { useIsDentalClinic } from "@/features/identity/modules/hooks/useIsDentalClinic";
import { useBudget } from "../hooks/useBudgets";
import type { BudgetStatus } from "../types/budget.types";
import { BUDGET_STATUS_LABELS } from "../utils/installments";

export const BUDGET_STATUS_PILL: Record<BudgetStatus, string> = {
  DRAFT: "bg-slate-100 text-slate-700 dark:bg-slate-500/15 dark:text-slate-300",
  APPROVED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300",
  LOST: "bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300",
};

type Props = {
  companyId: string;
  contactId: string;
  budgetId: string | null;
  onOpenChange: (open: boolean) => void;
};

/** Resumo do orçamento: procedimentos, dentes e valores, com atalho para a aba Orçamentos. */
export function BudgetDetailsDialog({ companyId, contactId, budgetId, onOpenChange }: Props) {
  const { data: budget, isLoading } = useBudget(companyId, contactId, budgetId);
  const dental = useIsDentalClinic();
  const date = budget?.approvedAt ?? budget?.updatedAt;

  return (
    <Dialog open={budgetId != null} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Orçamento {budget?.title ?? ""}</DialogTitle>
          <DialogDescription asChild>
            <div className="flex flex-wrap items-center gap-2">
              {date && (
                <span>
                  {budget?.approvedAt ? "Aprovado em" : "Atualizado em"}{" "}
                  {format(parseISO(date), "d 'de' MMM. 'de' yyyy", { locale: ptBR })}
                </span>
              )}
              {budget && (
                <span
                  className={cn(
                    "rounded-full px-2 py-0.5 text-xs font-medium",
                    BUDGET_STATUS_PILL[budget.status],
                  )}
                >
                  {BUDGET_STATUS_LABELS[budget.status]}
                </span>
              )}
            </div>
          </DialogDescription>
        </DialogHeader>

        {isLoading || !budget ? (
          <div className="flex items-center justify-center gap-2 py-10 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" /> Carregando…
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm font-medium">Procedimentos ({budget.items.length})</p>
            <div className="max-h-72 overflow-y-auto rounded-lg border">
              <table className="w-full text-sm">
                <thead className="sticky top-0 bg-muted/60 text-xs text-muted-foreground">
                  <tr>
                    <th className="px-3 py-2 text-left font-medium">Procedimento</th>
                    {dental && <th className="px-3 py-2 text-center font-medium">Dentes</th>}
                    <th className="px-3 py-2 text-right font-medium">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  {budget.items.map((item) => (
                    <tr key={item.procedureId} className="border-t even:bg-muted/20">
                      <td className="px-3 py-2">{item.name}</td>
                      {dental && (
                        <td className="px-3 py-2 text-center">
                          {item.teeth.length === 0 ? (
                            <span className="text-muted-foreground">—</span>
                          ) : (
                            <span className="inline-flex flex-wrap justify-center gap-1">
                              {item.teeth.map((t) => (
                                <span
                                  key={t.tooth}
                                  className="rounded bg-primary/10 px-1.5 py-0.5 text-xs font-semibold text-primary"
                                >
                                  {t.tooth}
                                  {t.faces.join("")}
                                </span>
                              ))}
                            </span>
                          )}
                        </td>
                      )}
                      <td className="px-3 py-2 text-right tabular-nums">
                        {centsToBRL(item.priceCents)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <dl className="ml-auto w-full max-w-xs space-y-1 text-sm">
              {budget.discountCents > 0 && (
                <>
                  <div className="flex justify-between text-muted-foreground">
                    <dt>Subtotal</dt>
                    <dd className="tabular-nums">{centsToBRL(budget.subtotalCents)}</dd>
                  </div>
                  <div className="flex justify-between text-muted-foreground">
                    <dt>Desconto</dt>
                    <dd className="tabular-nums">− {centsToBRL(budget.discountCents)}</dd>
                  </div>
                </>
              )}
              <div className="flex justify-between font-semibold">
                <dt>Total</dt>
                <dd className="tabular-nums">{centsToBRL(budget.totalCents)}</dd>
              </div>
            </dl>
          </div>
        )}

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Fechar
          </Button>
          <Button asChild>
            <Link href={`${ROUTES.PATIENTS}/${contactId}/orcamentos`}>Ver orçamento completo</Link>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
