"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ChevronRight, Loader2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePatient, useUpdatePatient } from "@/features/clinical/patients/hooks/usePatient";
import { PatientForm } from "@/features/clinical/patients/components/PatientForm";
import {
  contactToFormValues,
  formValuesToContactPayload,
  type PatientFormValues,
} from "@/features/clinical/patients/schemas/patient.schema";
import { ROUTES } from "@/lib/constants";

type ApiError = { response?: { status?: number; data?: { message?: string } } };

export default function EditPatientPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const patientId = params?.id;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: patient, isLoading, isError } = usePatient(companyId, patientId);
  const updatePatient = useUpdatePatient(companyId);
  const [serverError, setServerError] = useState<{
    field: keyof PatientFormValues;
    message: string;
  } | null>(null);

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
  const goBack = () => router.push(`${ROUTES.PATIENTS}/${patient.id}`);

  const handleSubmit = (values: PatientFormValues) => {
    setServerError(null);
    updatePatient.mutate(
      { id: patient.id, data: formValuesToContactPayload(values) },
      {
        onSuccess: goBack,
        onError: (error) => {
          const apiError = error as ApiError;
          if (apiError.response?.status === 409) {
            setServerError({
              field: "recordNumber",
              message: apiError.response.data?.message ?? "Nº de prontuário já em uso.",
            });
          }
        },
      },
    );
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
        <Link
          href={`${ROUTES.PATIENTS}/${patient.id}`}
          className="text-muted-foreground transition-colors hover:text-foreground"
        >
          {fullName || "Paciente"}
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-medium text-foreground">Editar</span>
      </nav>

      <h1 className="text-2xl font-bold tracking-tight">Editar paciente</h1>

      <PatientForm
        defaultValues={contactToFormValues(patient)}
        onSubmit={handleSubmit}
        isLoading={updatePatient.isPending}
        submitLabel="Salvar"
        onCancel={goBack}
        serverError={serverError}
      />
    </div>
  );
}
