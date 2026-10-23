"use client";

import { useParams } from "next/navigation";
import { PatientEvolutionsTab } from "@/features/clinical/evolutions/components/PatientEvolutionsTab";
import { usePatient } from "@/features/clinical/patients/hooks/usePatient";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { Skeleton } from "@/components/ui/skeleton";

export default function PatientEvolutionsPage() {
  const params = useParams<{ id: string }>();
  const contactId = params.id;
  const companyId = useCompanyId();
  const { data: patient, isLoading } = usePatient(companyId, contactId);

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  if (!patient || !companyId) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  return <PatientEvolutionsTab companyId={companyId} contactId={contactId} patient={patient} />;
}
