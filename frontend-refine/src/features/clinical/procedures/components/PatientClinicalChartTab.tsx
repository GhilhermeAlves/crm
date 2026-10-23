"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
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
  const { data: procedures = [], isLoading } = useProcedures(companyId, contactId);
  const [dentition, setDentition] = useState<Dentition>("PERMANENT");
  const [toothFilter, setToothFilter] = useState<number | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<PatientProcedureResponse | null>(null);
  const [completing, setCompleting] = useState<PatientProcedureResponse | null>(null);
  const summary = useMemo(() => toothStates(procedures, dentition), [procedures, dentition]);

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base">Odontograma</CardTitle>
          {canEdit && (
            <Button
              size="sm"
              onClick={() => {
                setEditing(null);
                setDrawerOpen(true);
              }}
            >
              + Novo procedimento
            </Button>
          )}
        </CardHeader>
        <CardContent>
          <Odontogram
            dentition={dentition}
            onDentitionChange={(value) => {
              setDentition(value);
              setToothFilter(null);
            }}
            summary={summary}
            selected={toothFilter == null ? [] : [toothFilter]}
            onToothClick={(tooth) =>
              setToothFilter((current) => (current === tooth ? null : tooth))
            }
          />
        </CardContent>
      </Card>
      <ProcedureList
        companyId={companyId}
        contactId={contactId}
        procedures={procedures}
        toothFilter={toothFilter}
        onClearTooth={() => setToothFilter(null)}
        onEdit={(procedure) => {
          setEditing(procedure);
          setDrawerOpen(true);
        }}
        onComplete={setCompleting}
        canEdit={canEdit}
      />
      <ProcedureDrawer
        companyId={companyId}
        contactId={contactId}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        procedure={editing}
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
