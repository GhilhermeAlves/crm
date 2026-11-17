"use client";

import { useState } from "react";
import { format } from "date-fns";
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
import { Card, CardContent } from "@/components/ui/card";
import { DataTable } from "@/components/ui/data-table";
import { centsToBRL } from "@/features/finance/utils/money";
import { useDeleteBudget, useLoseBudget } from "../hooks/useBudgets";
import type { BudgetSummary } from "../types/budget.types";
import { BUDGET_STATUS_LABELS } from "../utils/installments";

type Props = {
  companyId: string;
  contactId: string;
  budgets: BudgetSummary[];
  canEdit: boolean;
  onEdit: (budget: BudgetSummary) => void;
  onApprove: (budget: BudgetSummary) => void;
  onView: (budget: BudgetSummary) => void;
};

const formatDateTime = (iso: string) => {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? "—" : format(date, "dd/MM/yyyy HH:mm");
};

export function BudgetList({
  companyId,
  contactId,
  budgets,
  canEdit,
  onEdit,
  onApprove,
  onView,
}: Props) {
  const lose = useLoseBudget(companyId, contactId);
  const remove = useDeleteBudget(companyId, contactId);
  const [toLose, setToLose] = useState<BudgetSummary | null>(null);
  const [toDelete, setToDelete] = useState<BudgetSummary | null>(null);

  return (
    <Card>
      <CardContent className="pt-6">
        {budgets.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Nenhum orçamento para este paciente.
          </p>
        ) : (
          <DataTable
            tableId="budgets"
            data={budgets ?? []}
            bordered={false}
            columns={[
              {
                id: "titulo",
                header: "Título",
                className: "font-medium",
                sortValue: (b) => b.title,
                cell: (b) => <>{b.title}</>,
              },
              {
                id: "ultima-edicao",
                header: "Última edição",
                sortValue: (b) => b.updatedAt,
                cell: (b) => <>{formatDateTime(b.updatedAt)}</>,
              },
              {
                id: "editado-por",
                header: "Editado por",
                sortValue: (b) => b.updatedByName,
                cell: (b) => <>{b.updatedByName ?? "—"}</>,
              },
              { id: "total", header: "Total", cell: (b) => <>{centsToBRL(b.totalCents)}</> },
              {
                id: "status",
                header: "Status",
                cell: (b) => <Badge variant="outline">{BUDGET_STATUS_LABELS[b.status]}</Badge>,
              },
              {
                id: "acoes",
                header: "Ações",
                hideable: false,
                cell: (b) => (
                  <div className="flex flex-wrap items-center gap-1">
                    <Button size="sm" variant="ghost" onClick={() => onView(b)}>
                      Visualizar
                    </Button>
                    {canEdit && b.status === "DRAFT" && (
                      <>
                        <Button size="sm" variant="ghost" onClick={() => onEdit(b)}>
                          Editar
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => onApprove(b)}>
                          Aprovar
                        </Button>
                      </>
                    )}
                    {canEdit && (b.status === "DRAFT" || b.status === "APPROVED") && (
                      <Button size="sm" variant="ghost" onClick={() => setToLose(b)}>
                        Marcar como perdido
                      </Button>
                    )}
                    {canEdit && (b.status === "DRAFT" || b.status === "LOST") && (
                      <Button size="sm" variant="ghost" onClick={() => setToDelete(b)}>
                        Excluir
                      </Button>
                    )}
                  </div>
                ),
              },
            ]}
            keyExtractor={(b) => b.id}
          />
        )}
      </CardContent>

      <AlertDialog open={toLose != null} onOpenChange={(o) => !o && setToLose(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Marcar orçamento como perdido?</AlertDialogTitle>
            <AlertDialogDescription>
              {toLose?.status === "APPROVED"
                ? "As parcelas pendentes serão canceladas."
                : "O orçamento deixará de ser considerado em negociação."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (toLose) lose.mutate(toLose.id);
                setToLose(null);
              }}
            >
              Confirmar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={toDelete != null} onOpenChange={(o) => !o && setToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir orçamento?</AlertDialogTitle>
            <AlertDialogDescription>Esta ação não pode ser desfeita.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (toDelete) remove.mutate(toDelete.id);
                setToDelete(null);
              }}
            >
              Confirmar exclusão
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
