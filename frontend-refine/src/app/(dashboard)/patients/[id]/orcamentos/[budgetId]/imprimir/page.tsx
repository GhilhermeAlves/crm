"use client";

import { useEffect } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { BudgetPrintView } from "@/features/clinical/budgets/components/BudgetPrintView";
import { useBudget } from "@/features/clinical/budgets/hooks/useBudgets";
import { parseBlocks } from "@/features/clinical/budgets/utils/blocks";
import { usePatient } from "@/features/clinical/patients/hooks/usePatient";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";

export default function BudgetPrintPage() {
  const params = useParams<{ id: string; budgetId: string }>();
  const searchParams = useSearchParams();
  const companyId = useCompanyId();
  const contactId = params.id;
  const { data: budget } = useBudget(companyId ?? "", contactId, params.budgetId);
  const { data: patient } = usePatient(companyId, contactId);
  const ready = Boolean(budget && patient);

  useEffect(() => {
    if (ready && typeof window !== "undefined") window.print();
  }, [ready]);

  if (!budget || !patient) {
    return <p className="p-8 text-sm text-muted-foreground">Carregando...</p>;
  }

  return (
    <BudgetPrintView
      budget={budget}
      blocks={parseBlocks(searchParams.get("blocos"))}
      patientName={`${patient.firstName} ${patient.lastName}`.trim()}
    />
  );
}
