"use client";

import { useMemo, useState } from "react";
import { Trash2 } from "lucide-react";
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
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { BUDGET_STATUS_LABELS } from "@/features/clinical/budgets/utils/installments";
import { useDeleteProcedure } from "../hooks/useProcedures";
import type {
  PatientProcedureResponse,
  ProcedureState,
  TransactionStatus,
} from "../types/procedure.types";
import { formatTeeth, STATE_LABELS } from "../utils/teeth";

type Props = {
  companyId: string;
  contactId: string;
  procedures: PatientProcedureResponse[];
  canEdit: boolean;
  onEdit: (procedure: PatientProcedureResponse) => void;
  onComplete: (procedure: PatientProcedureResponse) => void;
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

export function ProcedureList({
  companyId,
  contactId,
  procedures,
  canEdit,
  onEdit,
  onComplete,
}: Props) {
  const remove = useDeleteProcedure(companyId, contactId);
  const [stateFilter, setStateFilter] = useState<ProcedureState | null>(null);
  const [category, setCategory] = useState(ALL);
  const [toDelete, setToDelete] = useState<PatientProcedureResponse | null>(null);

  const categories = useMemo(
    () =>
      Array.from(new Set(procedures.map((p) => p.category).filter((c): c is string => c != null))),
    [procedures],
  );

  const visible = procedures.filter(
    (p) =>
      (stateFilter == null || p.state === stateFilter) &&
      (category === ALL || p.category === category),
  );
  const filtered = stateFilter != null || category !== ALL;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Ficha geral</CardTitle>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            size="sm"
            variant={stateFilter == null ? "default" : "outline"}
            aria-label="Filtrar: Todos"
            onClick={() => setStateFilter(null)}
          >
            Todos
          </Button>
          {STATES.map((s) => (
            <Button
              key={s}
              size="sm"
              variant={stateFilter === s ? "default" : "outline"}
              aria-label={`Filtrar: ${STATE_LABELS[s]}`}
              onClick={() => setStateFilter(s)}
            >
              {STATE_LABELS[s]}
            </Button>
          ))}
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger className="w-48" aria-label="Tipo">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>Todos os tipos</SelectItem>
              {categories.map((c) => (
                <SelectItem key={c} value={c}>
                  {c}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardHeader>
      <CardContent>
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            {filtered ? "Nenhum procedimento para este filtro." : "Nenhum procedimento registrado."}
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Procedimento</TableHead>
                <TableHead>Dentes</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Valor</TableHead>
                <TableHead>Data</TableHead>
                <TableHead>Profissional</TableHead>
                <TableHead>Lançamento</TableHead>
                <TableHead>Orçamento</TableHead>
                {canEdit && <TableHead>Ações</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {visible.map((p) => {
                const paid = p.transaction?.status === "PAID";
                const budgetApproved = p.budget?.status === "APPROVED";
                return (
                  <TableRow key={p.id}>
                    <TableCell>
                      <div className="font-medium">{p.name}</div>
                      {p.category && (
                        <div className="text-xs text-muted-foreground">{p.category}</div>
                      )}
                    </TableCell>
                    <TableCell>{formatTeeth(p.teeth)}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{STATE_LABELS[p.state]}</Badge>
                    </TableCell>
                    <TableCell>{formatPrice(p.priceCents)}</TableCell>
                    <TableCell>{formatDate(p.performedAt)}</TableCell>
                    <TableCell>{p.professionalName ?? "—"}</TableCell>
                    <TableCell>
                      {p.transaction ? TRANSACTION_LABELS[p.transaction.status] : "—"}
                    </TableCell>
                    <TableCell>
                      {p.budget
                        ? `${p.budget.title} · ${BUDGET_STATUS_LABELS[p.budget.status]}`
                        : "—"}
                    </TableCell>
                    {canEdit && (
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Button size="sm" variant="ghost" onClick={() => onEdit(p)}>
                            Editar
                          </Button>
                          {p.state === "PLANNED" && (
                            <Button size="sm" variant="ghost" onClick={() => onComplete(p)}>
                              Marcar como realizado
                            </Button>
                          )}
                          <Button
                            size="icon"
                            variant="ghost"
                            aria-label="Excluir procedimento"
                            disabled={paid || budgetApproved}
                            title={
                              budgetApproved
                                ? "Procedimento de orçamento aprovado não pode ser excluído"
                                : paid
                                  ? "Procedimento com pagamento registrado não pode ser excluído"
                                  : undefined
                            }
                            onClick={() => setToDelete(p)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    )}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </CardContent>

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
