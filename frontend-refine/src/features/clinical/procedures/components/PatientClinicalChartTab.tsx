"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useIsDentalClinic } from "@/features/identity/modules/hooks/useIsDentalClinic";
import { BudgetDetailsDialog } from "@/features/clinical/budgets/components/BudgetDetailsDialog";
import { BudgetFormDialog } from "@/features/clinical/budgets/components/BudgetFormDialog";
import { useProcedures } from "../hooks/useProcedures";
import type { Dentition, PatientProcedureResponse } from "../types/procedure.types";
import { toothStates } from "../utils/teeth";
import { CompleteProcedureDialog } from "./CompleteProcedureDialog";
import { Odontogram } from "./Odontogram";
import { ProcedureDrawer } from "./ProcedureDrawer";
import { ProcedureList } from "./ProcedureList";

export function PatientClinicalChartTab({
  companyId,
  contactId,
}: {
  companyId: string;
  contactId: string;
}) {
  const { can } = useAuthorization();
  const canEdit = can("contact:update");
  // Odontograma, dentes e faces só para clínicas de odontologia.
  const dental = useIsDentalClinic();
  const { data: procedures = [], isLoading } = useProcedures(companyId, contactId);
  const [dentition, setDentition] = useState<Dentition>("PERMANENT");
  const [initialTooth, setInitialTooth] = useState<number | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<PatientProcedureResponse | null>(null);
  const [completing, setCompleting] = useState<PatientProcedureResponse | null>(null);
  const [budgeting, setBudgeting] = useState<string | null>(null);
  const [viewingBudget, setViewingBudget] = useState<string | null>(null);
  const summary = useMemo(() => toothStates(procedures, dentition), [procedures, dentition]);

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base">{dental ? "Odontograma" : "Procedimentos"}</CardTitle>
          {canEdit && (
            <Button
              size="sm"
              onClick={() => {
                setEditing(null);
                setInitialTooth(null);
                setDrawerOpen(true);
              }}
            >
              + Novo procedimento
            </Button>
          )}
        </CardHeader>
        {dental && (
          <CardContent>
            <Odontogram
              dentition={dentition}
              onDentitionChange={setDentition}
              summary={summary}
              selected={[]}
              onToothClick={(tooth) => {
                if (!canEdit) return;
                setEditing(null);
                setInitialTooth(tooth);
                setDrawerOpen(true);
              }}
            />
          </CardContent>
        )}
      </Card>
      <ProcedureList
        companyId={companyId}
        contactId={contactId}
        procedures={procedures}
        onEdit={(procedure) => {
          setEditing(procedure);
          setInitialTooth(null);
          setDrawerOpen(true);
        }}
        onComplete={setCompleting}
        onBudget={(procedure) => setBudgeting(procedure.id)}
        onViewBudget={setViewingBudget}
        canEdit={canEdit}
        showTeeth={dental}
      />
      <ProcedureDrawer
        companyId={companyId}
        contactId={contactId}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        procedure={editing}
        initialTooth={initialTooth}
        initialDentition={dentition}
        showTeeth={dental}
      />
      <BudgetFormDialog
        companyId={companyId}
        contactId={contactId}
        open={budgeting != null}
        onOpenChange={(open) => !open && setBudgeting(null)}
        initialProcedureId={budgeting}
      />
      <BudgetDetailsDialog
        companyId={companyId}
        contactId={contactId}
        budgetId={viewingBudget}
        onOpenChange={(open) => !open && setViewingBudget(null)}
      />
      <CompleteProcedureDialog
        companyId={companyId}
        contactId={contactId}
        procedure={completing}
        onOpenChange={(open) => !open && setCompleting(null)}
      />
    </div>
  );
}
