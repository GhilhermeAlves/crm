"use client";

import { useEffect } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { BudgetPrintView } from "@/features/clinical/budgets/components/BudgetPrintView";
import { useBudget } from "@/features/clinical/budgets/hooks/useBudgets";
import { parseBlocks } from "@/features/clinical/budgets/utils/blocks";
import { usePatient } from "@/features/clinical/patients/hooks/usePatient";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { useCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";

export default function BudgetPrintPage() {
  const params = useParams<{ id: string; budgetId: string }>();
  const searchParams = useSearchParams();
  const companyId = useCompanyId();
  const contactId = params.id;
  const { data: budget } = useBudget(companyId ?? "", contactId, params.budgetId);
  const { data: patient } = usePatient(companyId, contactId);
  const { data: modules } = useCompanyModules(companyId);
  // Espera o segmento: a coluna de dentes só sai na impressão de clínicas de odontologia.
  const ready = Boolean(budget && patient && modules);

  useEffect(() => {
    if (ready && typeof window !== "undefined") window.print();
  }, [ready]);

  if (!budget || !patient || !modules) {
    return <p className="p-8 text-sm text-muted-foreground">Carregando...</p>;
  }

  return (
    <BudgetPrintView
      budget={budget}
      blocks={parseBlocks(searchParams.get("blocos"))}
      patientName={`${patient.firstName} ${patient.lastName}`.trim()}
      showTeeth={modules.segment === "DENTAL"}
    />
  );
}
