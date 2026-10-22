"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChevronRight } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useCreatePatient } from "@/features/clinical/patients/hooks/usePatient";
import { PatientForm } from "@/features/clinical/patients/components/PatientForm";
import {
  formValuesToContactPayload,
  type PatientFormValues,
} from "@/features/clinical/patients/schemas/patient.schema";
import { ROUTES } from "@/lib/constants";

type ApiError = { response?: { status?: number; data?: { message?: string } } };

export default function NewPatientPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const createPatient = useCreatePatient(companyId);
  const [serverError, setServerError] = useState<{
    field: keyof PatientFormValues;
    message: string;
  } | null>(null);

  const handleSubmit = (values: PatientFormValues) => {
    setServerError(null);
    createPatient.mutate(formValuesToContactPayload(values), {
      onSuccess: (created) => router.push(`${ROUTES.PATIENTS}/${created.id}`),
      onError: (error) => {
        const apiError = error as ApiError;
        if (apiError.response?.status === 409) {
          setServerError({
            field: "recordNumber",
            message: apiError.response.data?.message ?? "Nº de prontuário já em uso.",
          });
        }
      },
    });
  };

  return (
    <div className="space-y-6">
      <nav aria-label="Trilha de navegação" className="flex items-center gap-1 text-sm">
        <Link
          href={ROUTES.PATIENTS}
          className="text-muted-foreground transition-colors hover:text-foreground"
        >
          Pacientes
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-medium text-foreground">Novo paciente</span>
      </nav>

      <h1 className="text-2xl font-bold tracking-tight">Novo paciente</h1>

      <PatientForm
        defaultValues={{}}
        onSubmit={handleSubmit}
        isLoading={createPatient.isPending}
        submitLabel="Salvar"
        onCancel={() => router.push(ROUTES.PATIENTS)}
        serverError={serverError}
      />
    </div>
  );
}
