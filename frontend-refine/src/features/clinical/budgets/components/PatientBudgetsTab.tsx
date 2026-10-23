"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useBudget, useBudgets } from "../hooks/useBudgets";
import type { BudgetSummary } from "../types/budget.types";
import { ApproveBudgetDialog } from "./ApproveBudgetDialog";
import { BudgetFormDialog } from "./BudgetFormDialog";
import { BudgetList } from "./BudgetList";
import { BudgetViewDialog } from "./BudgetViewDialog";

export function PatientBudgetsTab({
  companyId,
  contactId,
}: {
  companyId: string;
  contactId: string;
}) {
  const { can } = useAuthorization();
  const canEdit = can("contact:update");
  const { data: budgets = [], isLoading } = useBudgets(companyId, contactId);
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [approving, setApproving] = useState<BudgetSummary | null>(null);
  const [viewingId, setViewingId] = useState<string | null>(null);
  const { data: editing } = useBudget(companyId, contactId, editingId);

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base">Orçamentos</CardTitle>
          {canEdit && (
            <Button
              size="sm"
              onClick={() => {
                setEditingId(null);
                setFormOpen(true);
              }}
            >
              + Novo orçamento
            </Button>
          )}
        </CardHeader>
      </Card>
      <BudgetList
        companyId={companyId}
        contactId={contactId}
        budgets={budgets}
        canEdit={canEdit}
        onEdit={(budget) => {
          setEditingId(budget.id);
          setFormOpen(true);
        }}
        onApprove={setApproving}
        onView={(budget) => setViewingId(budget.id)}
      />
      <BudgetFormDialog
        companyId={companyId}
        contactId={contactId}
        open={formOpen && (editingId == null || editing != null)}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) setEditingId(null);
        }}
        budget={editingId ? editing : null}
      />
      <ApproveBudgetDialog
        companyId={companyId}
        contactId={contactId}
        budget={approving}
        onOpenChange={(open) => !open && setApproving(null)}
      />
      <BudgetViewDialog
        contactId={contactId}
        budgetId={viewingId}
        onOpenChange={(open) => !open && setViewingId(null)}
      />
    </div>
  );
}
