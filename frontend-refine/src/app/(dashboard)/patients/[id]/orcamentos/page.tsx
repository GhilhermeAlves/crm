"use client";

import { useParams } from "next/navigation";
import { PatientBudgetsTab } from "@/features/clinical/budgets/components/PatientBudgetsTab";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";

export default function PatientBudgetsPage() {
  const params = useParams<{ id: string }>();
  const contactId = params.id;
  const companyId = useCompanyId();

  if (!companyId) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  return <PatientBudgetsTab companyId={companyId} contactId={contactId} />;
}
