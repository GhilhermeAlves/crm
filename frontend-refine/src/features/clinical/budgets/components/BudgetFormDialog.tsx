"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { Textarea } from "@/components/ui/textarea";
import { useProcedures } from "@/features/clinical/procedures/hooks/useProcedures";
import { formatTeeth } from "@/features/clinical/procedures/utils/teeth";
import { centsToBRL } from "@/features/finance/utils/money";
import { useCreateBudget, useUpdateBudget } from "../hooks/useBudgets";
import type { BudgetDetail } from "../types/budget.types";

type Props = {
  companyId: string;
  contactId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  budget?: BudgetDetail | null;
};

const toCents = (value: string) => Math.round(Number(value || 0) * 100);

export function BudgetFormDialog({ companyId, contactId, open, onOpenChange, budget }: Props) {
  const uid = useId();
  const { data: procedures } = useProcedures(companyId, contactId);
  const create = useCreateBudget(companyId, contactId);
  const update = useUpdateBudget(companyId, contactId);

  // Reset só ao abrir ou trocar de orçamento: um refetch (nova referência) não apaga a edição.
  const budgetRef = useRef(budget);
  budgetRef.current = budget;
  const budgetId = budget?.id;

  const [title, setTitle] = useState("");
  const [notes, setNotes] = useState("");
  const [discount, setDiscount] = useState("0");
  // procedureId -> valor digitado (R$); presença da chave = item marcado.
  const [prices, setPrices] = useState<Record<string, string>>({});

  const available = useMemo(
    () =>
      (procedures ?? []).filter(
        (p) => p.state === "PLANNED" && (p.budget == null || p.budget.id === budget?.id),
      ),
    [procedures, budget?.id],
  );

  useEffect(() => {
    if (!open) return;
    const budget = budgetRef.current;
    setTitle(budget?.title ?? "");
    setNotes(budget?.notes ?? "");
    setDiscount(String((budget?.discountCents ?? 0) / 100));
    setPrices(
      Object.fromEntries(
        (budget?.items ?? []).map((i) => [i.procedureId, String(i.priceCents / 100)]),
      ),
    );
  }, [open, budgetId]);

  const toggle = (id: string, priceCents: number, checked: boolean) =>
    setPrices((prev) => {
      const next = { ...prev };
      if (checked) next[id] = String(priceCents / 100);
      else delete next[id];
      return next;
    });

  const selected = available.filter((p) => p.id in prices);
  const subtotalCents = selected.reduce((sum, p) => sum + toCents(prices[p.id]), 0);
  const discountCents = toCents(discount);
  const discountTooHigh = discountCents > subtotalCents;
  const totalCents = subtotalCents - discountCents;
  const pending = create.isPending || update.isPending;
  const canSave =
    title.trim() !== "" &&
    selected.length > 0 &&
    !discountTooHigh &&
    discountCents >= 0 &&
    selected.every((p) => toCents(prices[p.id]) >= 0) &&
    !pending;

  const save = () => {
    const payload = {
      title: title.trim(),
      procedureIds: selected.map((p) => p.id),
      discountCents,
      notes: notes.trim() || null,
      itemPrices: Object.fromEntries(selected.map((p) => [p.id, toCents(prices[p.id])])),
    };
    const options = { onSuccess: () => onOpenChange(false) };
    if (budget) update.mutate({ budgetId: budget.id, payload }, options);
    else create.mutate(payload, options);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{budget ? "Editar orçamento" : "Novo orçamento"}</DialogTitle>
          <DialogDescription>Selecione os procedimentos e ajuste os valores.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor={`${uid}-title`}>Título</Label>
            <Input id={`${uid}-title`} value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>

          {available.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Nenhum procedimento &quot;A realizar&quot; disponível. Cadastre na Ficha clínica.
            </p>
          ) : (
            <ul className="space-y-2">
              {available.map((p) => {
                const checked = p.id in prices;
                return (
                  <li key={p.id} className="flex items-center gap-3">
                    <Checkbox
                      id={`${uid}-item-${p.id}`}
                      checked={checked}
                      onCheckedChange={(v) => toggle(p.id, p.priceCents, v === true)}
                    />
                    <Label htmlFor={`${uid}-item-${p.id}`} className="flex-1 font-normal">
                      {p.name} — {formatTeeth(p.teeth)}
                    </Label>
                    {checked && (
                      <Input
                        aria-label={`Valor (R$) de ${p.name}`}
                        className="w-28"
                        type="number"
                        step="0.01"
                        min="0"
                        value={prices[p.id]}
                        onChange={(e) => setPrices((prev) => ({ ...prev, [p.id]: e.target.value }))}
                      />
                    )}
                  </li>
                );
              })}
            </ul>
          )}

          <div className="space-y-2">
            <Label htmlFor={`${uid}-discount`}>Desconto (R$)</Label>
            <Input
              id={`${uid}-discount`}
              type="number"
              step="0.01"
              min="0"
              value={discount}
              onChange={(e) => setDiscount(e.target.value)}
            />
            {discountTooHigh && (
              <p className="text-sm text-destructive">
                O desconto não pode ser maior que o subtotal.
              </p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor={`${uid}-notes`}>Observações</Label>
            <Textarea
              id={`${uid}-notes`}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <dl className="space-y-1 rounded-md border p-3 text-sm tabular-nums">
            <div className="flex justify-between">
              <dt>Subtotal</dt>
              <dd data-testid="budget-subtotal">{centsToBRL(subtotalCents)}</dd>
            </div>
            <div className="flex justify-between">
              <dt>Desconto</dt>
              <dd data-testid="budget-discount">{centsToBRL(discountCents)}</dd>
            </div>
            <div className="flex justify-between font-semibold">
              <dt>Total</dt>
              <dd data-testid="budget-total">{centsToBRL(totalCents)}</dd>
            </div>
          </dl>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={save} disabled={!canSave}>
            Salvar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
