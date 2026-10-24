"use client";

import { useMemo, useState } from "react";
import { format } from "date-fns";
import {
  CalendarCheck,
  CheckCheck,
  Eye,
  MoreVertical,
  Pencil,
  Plus,
  Trash2,
  type LucideIcon,
} from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { BUDGET_STATUS_LABELS } from "@/features/clinical/budgets/utils/installments";
import { useCompleteProcedure, useDeleteProcedure } from "../hooks/useProcedures";
import type {
  PatientProcedureResponse,
  ProcedureState,
  TransactionStatus,
} from "../types/procedure.types";
import { formatTeeth, STATE_FILL, STATE_LABELS, STATE_PILL } from "../utils/teeth";

type Props = {
  companyId: string;
  contactId: string;
  procedures: PatientProcedureResponse[];
  canEdit: boolean;
  onEdit: (procedure: PatientProcedureResponse) => void;
  onComplete: (procedure: PatientProcedureResponse) => void;
  /** "+ Orçar": abre um orçamento novo com o procedimento marcado. */
  onBudget?: (procedure: PatientProcedureResponse) => void;
  /** Link "Orçamento": abre o resumo do orçamento. */
  onViewBudget?: (budgetId: string) => void;
};

const ALL = "all";
const STATES: ProcedureState[] = ["PLANNED", "DONE", "PRE_EXISTING"];
const TRANSACTION_LABELS: Record<TransactionStatus, string> = {
  PENDING: "Pendente",
  PAID: "Pago",
  CANCELLED: "Cancelado",
};

const formatPrice = (cents: number) =>
  (cents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const formatDate = (iso: string | null) => {
  if (!iso) return "—";
  const [year, month, day] = iso.slice(0, 10).split("-");
  return `${day}/${month}/${year}`;
};

/** Dentes no formato do Capim: "37 M, O, D". */
const formatToothColumn = (p: PatientProcedureResponse) =>
  p.teeth.length === 0
    ? "—"
    : p.teeth
        .map(({ tooth, faces }) => (faces.length ? `${tooth} ${faces.join(", ")}` : String(tooth)))
        .join(" · ");

function StatePill({ procedure }: { procedure: PatientProcedureResponse }) {
  const text =
    procedure.state === "DONE" && procedure.performedAt
      ? `Realizado em ${formatDate(procedure.performedAt)}`
      : STATE_LABELS[procedure.state];
  return (
    <span
      className={cn(
        "inline-flex whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold",
        STATE_PILL[procedure.state],
      )}
    >
      {text}
    </span>
  );
}

function MenuItem({
  icon: Icon,
  label,
  onClick,
  disabled,
  title,
  destructive,
}: {
  icon: LucideIcon;
  label: string;
  onClick: () => void;
  disabled?: boolean;
  title?: string;
  destructive?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={cn(
        "flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors",
        "hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent",
        destructive && "text-red-600 dark:text-red-400",
      )}
    >
      <Icon className="h-4 w-4 shrink-0" />
      {label}
    </button>
  );
}

export function ProcedureList({
  companyId,
  contactId,
  procedures,
  canEdit,
  onEdit,
  onComplete,
  onBudget,
  onViewBudget,
}: Props) {
  const remove = useDeleteProcedure(companyId, contactId);
  const complete = useCompleteProcedure(companyId, contactId);
  const [stateFilter, setStateFilter] = useState<ProcedureState | null>(null);
  const [category, setCategory] = useState(ALL);
  const [toDelete, setToDelete] = useState<PatientProcedureResponse | null>(null);
  const [details, setDetails] = useState<PatientProcedureResponse | null>(null);
  const [menuFor, setMenuFor] = useState<string | null>(null);

  const categories = useMemo(
    () =>
      Array.from(new Set(procedures.map((p) => p.category).filter((c): c is string => c != null))),
    [procedures],
  );

  const byCategory = procedures.filter((p) => category === ALL || p.category === category);
  const visible = byCategory.filter((p) => stateFilter == null || p.state === stateFilter);
  const filtered = stateFilter != null || category !== ALL;
  const countOf = (state: ProcedureState | null) =>
    byCategory.filter((p) => state == null || p.state === state).length;

  const completeToday = (p: PatientProcedureResponse) => {
    const today = format(new Date(), "yyyy-MM-dd");
    const generate = p.generateTransactionOnDone && p.budget?.status !== "APPROVED";
    complete.mutate({
      procedureId: p.id,
      payload: {
        performedAt: today,
        professionalId: p.professionalId,
        generateTransaction: generate,
        dueDate: generate ? today : null,
      },
    });
  };

  const filters: { value: ProcedureState | null; label: string }[] = [
    { value: null, label: "Ficha geral" },
    ...STATES.map((s) => ({ value: s, label: STATE_LABELS[s] })),
  ];

  return (
    <Card>
      <CardHeader className="space-y-4">
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {filters.map(({ value, label }) => {
            const active = stateFilter === value;
            return (
              <button
                key={label}
                type="button"
                aria-label={`Filtrar: ${value == null ? "Todos" : label}`}
                aria-pressed={active}
                onClick={() => setStateFilter(value)}
                className={cn(
                  "flex items-center justify-between gap-2 rounded-lg border px-3 py-2 text-sm transition-colors",
                  active
                    ? "border-primary bg-primary/5 font-medium text-primary"
                    : "text-muted-foreground hover:border-primary/40 hover:text-foreground",
                )}
              >
                <span className="flex items-center gap-2">
                  {value == null ? (
                    <span
                      className={cn(
                        "h-2.5 w-2.5 rounded-full border-2",
                        active ? "border-primary bg-primary" : "border-muted-foreground/50",
                      )}
                    />
                  ) : (
                    <svg viewBox="0 0 10 10" className="h-2.5 w-2.5" aria-hidden>
                      <circle cx="5" cy="5" r="5" className={STATE_FILL[value]} />
                    </svg>
                  )}
                  {label}
                </span>
                <span className="rounded-full bg-muted px-1.5 text-xs tabular-nums">
                  {countOf(value)}
                </span>
              </button>
            );
          })}
        </div>
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger aria-label="Tipo">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>Todos os tipos ({procedures.length})</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c} value={c}>
                {c}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <CardTitle className="text-lg">Ficha Geral</CardTitle>
      </CardHeader>
      <CardContent>
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            {filtered ? "Nenhum procedimento para este filtro." : "Nenhum procedimento registrado."}
          </p>
        ) : (
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted/60 text-xs text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Procedimento</th>
                  <th className="px-4 py-3 text-left font-medium">Dente</th>
                  <th className="px-4 py-3 text-left font-medium">Estado</th>
                  <th className="px-4 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((p) => {
                  const paid = p.transaction?.status === "PAID";
                  const budgetApproved = p.budget?.status === "APPROVED";
                  return (
                    <tr key={p.id} className="border-t transition-colors hover:bg-muted/30">
                      <td className="px-4 py-3">
                        <div className="font-medium">{p.name}</div>
                        {p.category && (
                          <div className="text-xs text-muted-foreground">{p.category}</div>
                        )}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">
                        {formatToothColumn(p)}
                      </td>
                      <td className="px-4 py-3">
                        <StatePill procedure={p} />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-2">
                          {p.budget ? (
                            <button
                              type="button"
                              title={`${p.budget.title} · ${BUDGET_STATUS_LABELS[p.budget.status]}`}
                              onClick={() => p.budget && onViewBudget?.(p.budget.id)}
                              className="whitespace-nowrap text-sm font-medium text-primary underline underline-offset-2 hover:text-primary/80"
                            >
                              Orçamento
                            </button>
                          ) : (
                            canEdit &&
                            p.state === "PLANNED" &&
                            onBudget && (
                              <Button
                                size="sm"
                                variant="outline"
                                className="gap-1 border-primary/40 text-primary hover:bg-primary/5 hover:text-primary"
                                onClick={() => onBudget(p)}
                              >
                                <Plus className="h-3.5 w-3.5" /> Orçar
                              </Button>
                            )
                          )}
                          <Popover
                            open={menuFor === p.id}
                            onOpenChange={(open) => setMenuFor(open ? p.id : null)}
                          >
                            <PopoverTrigger asChild>
                              <Button
                                size="icon"
                                variant="outline"
                                className="h-8 w-8"
                                aria-label={`Ações de ${p.name}`}
                              >
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </PopoverTrigger>
                            <PopoverContent align="end" className="w-64 p-1">
                              {(() => {
                                const run = (fn: () => void) => () => {
                                  setMenuFor(null);
                                  fn();
                                };
                                return (
                                  <>
                                    {canEdit && p.state === "PLANNED" && (
                                      <>
                                        <MenuItem
                                          icon={CheckCheck}
                                          label="Marcar como realizado hoje"
                                          onClick={run(() => completeToday(p))}
                                        />
                                        <MenuItem
                                          icon={CalendarCheck}
                                          label="Marcar como realizado em…"
                                          onClick={run(() => onComplete(p))}
                                        />
                                      </>
                                    )}
                                    <MenuItem
                                      icon={Eye}
                                      label="Visualizar detalhes"
                                      onClick={run(() => setDetails(p))}
                                    />
                                    {canEdit && (
                                      <>
                                        <MenuItem
                                          icon={Pencil}
                                          label="Editar procedimento"
                                          onClick={run(() => onEdit(p))}
                                        />
                                        <MenuItem
                                          icon={Trash2}
                                          label="Excluir procedimento"
                                          destructive
                                          disabled={paid || budgetApproved}
                                          title={
                                            budgetApproved
                                              ? "Procedimento de orçamento aprovado não pode ser excluído"
                                              : paid
                                                ? "Procedimento com pagamento registrado não pode ser excluído"
                                                : undefined
                                          }
                                          onClick={run(() => setToDelete(p))}
                                        />
                                      </>
                                    )}
                                  </>
                                );
                              })()}
                            </PopoverContent>
                          </Popover>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>

      <Dialog open={details != null} onOpenChange={(o) => !o && setDetails(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{details?.name}</DialogTitle>
            <DialogDescription>{details?.category ?? "Procedimento"}</DialogDescription>
          </DialogHeader>
          {details && (
            <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
              <dt className="text-muted-foreground">Estado</dt>
              <dd>
                <StatePill procedure={details} />
              </dd>
              <dt className="text-muted-foreground">Dentes</dt>
              <dd>{formatTeeth(details.teeth)}</dd>
              <dt className="text-muted-foreground">Valor</dt>
              <dd className="tabular-nums">{formatPrice(details.priceCents)}</dd>
              <dt className="text-muted-foreground">Profissional</dt>
              <dd>{details.professionalName ?? "—"}</dd>
              <dt className="text-muted-foreground">Lançamento</dt>
              <dd>{details.transaction ? TRANSACTION_LABELS[details.transaction.status] : "—"}</dd>
              <dt className="text-muted-foreground">Orçamento</dt>
              <dd>
                {details.budget
                  ? `${details.budget.title} · ${BUDGET_STATUS_LABELS[details.budget.status]}`
                  : "—"}
              </dd>
              {details.notes && (
                <>
                  <dt className="text-muted-foreground">Observações</dt>
                  <dd className="whitespace-pre-wrap">{details.notes}</dd>
                </>
              )}
            </dl>
          )}
        </DialogContent>
      </Dialog>

      <AlertDialog open={toDelete != null} onOpenChange={(o) => !o && setToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir procedimento?</AlertDialogTitle>
            <AlertDialogDescription>Esta ação não pode ser desfeita.</AlertDialogDescription>
          </AlertDialogHeader>
          {toDelete?.transaction?.status === "PENDING" && (
            <p className="text-sm">
              O lançamento financeiro pendente deste procedimento será cancelado.
            </p>
          )}
          {toDelete?.budget?.status === "DRAFT" && (
            <p className="text-sm">
              O procedimento será removido do orçamento {toDelete.budget.title}.
            </p>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (toDelete) remove.mutate(toDelete.id);
                setToDelete(null);
              }}
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
