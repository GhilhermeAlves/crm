"use client";

import { useParams } from "next/navigation";
import { PatientClinicalChartTab } from "@/features/clinical/procedures/components/PatientClinicalChartTab";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";

export default function PatientClinicalChartPage() {
  const params = useParams<{ id: string }>();
  const contactId = params.id;
  const companyId = useCompanyId();

  if (!companyId) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  return <PatientClinicalChartTab companyId={companyId} contactId={contactId} />;
}
