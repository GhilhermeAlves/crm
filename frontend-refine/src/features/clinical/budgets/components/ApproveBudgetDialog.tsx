"use client";

import { useEffect, useId, useMemo, useState } from "react";
import { format, isValid, parse } from "date-fns";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PAYMENT_METHOD_LABELS } from "@/features/finance/types/finance.types";
import { centsToBRL } from "@/features/finance/utils/money";
import { useApproveBudget } from "../hooks/useBudgets";
import { splitInstallments } from "../utils/installments";
import type { BudgetDetail, BudgetSummary, PaymentMethod } from "../types/budget.types";

type Props = {
  companyId: string;
  contactId: string;
  budget: BudgetDetail | BudgetSummary | null;
  onOpenChange: (open: boolean) => void;
};

const today = () => format(new Date(), "yyyy-MM-dd");
const MAX_INSTALLMENTS = 24;
const clampInstallments = (n: number) =>
  Math.min(MAX_INSTALLMENTS, Math.max(1, Number.isFinite(n) ? Math.trunc(n) : 1));
const parseDate = (value: string) => parse(value, "yyyy-MM-dd", new Date());

export function ApproveBudgetDialog({ companyId, contactId, budget, onOpenChange }: Props) {
  const uid = useId();
  const approve = useApproveBudget(companyId, contactId);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("PIX");
  const [installments, setInstallments] = useState(1);
  const [firstDueDate, setFirstDueDate] = useState(today());

  useEffect(() => {
    if (!budget) return;
    setPaymentMethod("PIX");
    setInstallments(1);
    setFirstDueDate(today());
  }, [budget]);

  const totalCents = budget?.totalCents ?? 0;
  const validDate = firstDueDate !== "" && isValid(parseDate(firstDueDate));
  const preview = useMemo(
    () =>
      validDate && totalCents > 0 ? splitInstallments(totalCents, installments, firstDueDate) : [],
    [validDate, totalCents, installments, firstDueDate],
  );

  const confirm = () => {
    if (!budget) return;
    approve.mutate(
      { budgetId: budget.id, payload: { paymentMethod, installments, firstDueDate } },
      { onSuccess: () => onOpenChange(false) },
    );
  };

  return (
    <Dialog open={budget != null} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Aprovar orçamento</DialogTitle>
          <DialogDescription>
            {budget?.title} — {centsToBRL(totalCents)}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor={`${uid}-method`}>Forma de pagamento</Label>
            <Select
              value={paymentMethod}
              onValueChange={(v) => setPaymentMethod(v as PaymentMethod)}
            >
              <SelectTrigger id={`${uid}-method`}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {(Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[]).map((m) => (
                  <SelectItem key={m} value={m}>
                    {PAYMENT_METHOD_LABELS[m]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor={`${uid}-count`}>Parcelas</Label>
              <Input
                id={`${uid}-count`}
                type="number"
                min={1}
                max={MAX_INSTALLMENTS}
                step={1}
                value={installments}
                onChange={(e) => setInstallments(clampInstallments(Number(e.target.value)))}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor={`${uid}-due`}>1º vencimento</Label>
              <Input
                id={`${uid}-due`}
                type="date"
                value={firstDueDate}
                onChange={(e) => setFirstDueDate(e.target.value)}
              />
            </div>
          </div>
          {totalCents === 0 ? (
            <p className="text-sm text-muted-foreground">
              Orçamento sem valor: nenhuma parcela será gerada.
            </p>
          ) : (
            <ul className="space-y-1 text-sm tabular-nums">
              {preview.map((p, i) => (
                <li key={p.dueDate}>
                  {i + 1}ª parcela — {format(parseDate(p.dueDate), "dd/MM/yyyy")} —{" "}
                  {centsToBRL(p.amountCents)}
                </li>
              ))}
            </ul>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={confirm} disabled={approve.isPending || !validDate}>
            Aprovar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
