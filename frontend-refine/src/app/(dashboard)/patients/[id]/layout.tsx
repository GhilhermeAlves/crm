"use client";

import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import { ChevronRight, Loader2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePatient } from "@/features/clinical/patients/hooks/usePatient";
import { PatientHeader } from "@/features/clinical/patients/components/PatientHeader";
import { PatientTabs } from "@/features/clinical/patients/components/PatientTabs";
import { ROUTES } from "@/lib/constants";

export default function PatientRecordLayout({ children }: { children: React.ReactNode }) {
  const params = useParams<{ id: string }>();
  const pathname = usePathname();
  const patientId = params?.id;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: patient, isLoading, isError } = usePatient(companyId, patientId);

  // A edição tem cabeçalho e trilha próprios.
  if (pathname?.endsWith("/edit")) {
    return <>{children}</>;
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando paciente…
      </div>
    );
  }

  if (isError || !patient) {
    return (
      <p className="py-20 text-center text-sm text-destructive">
        Não foi possível carregar este paciente.
      </p>
    );
  }

  const fullName = [patient.firstName, patient.lastName].filter(Boolean).join(" ");

  return (
    <div className="space-y-6">
      <nav aria-label="Trilha de navegação" className="flex items-center gap-1 text-sm">
        <Link
          href={ROUTES.DASHBOARD}
          className="text-muted-foreground transition-colors hover:text-foreground"
        >
          Início
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
        <Link
          href={ROUTES.PATIENTS}
          className="text-muted-foreground transition-colors hover:text-foreground"
        >
          Pacientes
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-medium text-foreground">{fullName || "Paciente"}</span>
      </nav>

      <PatientHeader contact={patient} />
      <PatientTabs contactId={patient.id} />

      <div>{children}</div>
    </div>
  );
}
