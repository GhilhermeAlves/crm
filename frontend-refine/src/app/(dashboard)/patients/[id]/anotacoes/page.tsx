"use client";

import { useParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePatient } from "@/features/clinical/patients/hooks/usePatient";
import { PatientNotes } from "@/features/clinical/patients/components/PatientNotes";

export default function PatientNotesPage() {
  const params = useParams<{ id: string }>();
  const contactId = params?.id;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: contact, isLoading } = usePatient(companyId, contactId);

  if (isLoading || !contact) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando anotações…
      </div>
    );
  }

  return <PatientNotes contact={contact} />;
}
