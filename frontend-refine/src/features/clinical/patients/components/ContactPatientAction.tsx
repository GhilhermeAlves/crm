"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { HeartPulse, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { useContactPermissions } from "@/features/masterdata/contacts/hooks/useContacts";
import { ROUTES } from "@/lib/constants";
import { useMakePatient } from "../hooks/usePatient";

/**
 * Ponte Contato → Paciente no Customer 360. Só existe em empresas com o
 * módulo PATIENTS: fora da área da saúde o contato é apenas contato.
 */
export function ContactPatientAction({
  companyId,
  contactId,
  isPatient,
}: {
  companyId: string | null;
  contactId: string;
  isPatient: boolean;
}) {
  const router = useRouter();
  const { enabledModules } = useActiveCompanyModules();
  const { canUpdate } = useContactPermissions();
  const makePatient = useMakePatient(companyId);

  // Só mostra com os módulos já carregados: nunca oferecer "Tornar paciente"
  // por engano numa empresa fora da área da saúde.
  if (!enabledModules || !enabledModules.includes("PATIENTS")) {
    return null;
  }

  if (isPatient) {
    return (
      <Button size="sm" variant="outline" asChild>
        <Link href={`${ROUTES.PATIENTS}/${contactId}`}>
          <HeartPulse className="mr-1 h-4 w-4" /> Ver ficha do paciente
        </Link>
      </Button>
    );
  }

  if (!canUpdate) {
    return null;
  }

  return (
    <Button
      size="sm"
      variant="outline"
      disabled={makePatient.isPending}
      onClick={() =>
        makePatient.mutate(contactId, {
          onSuccess: () => router.push(`${ROUTES.PATIENTS}/${contactId}`),
        })
      }
    >
      {makePatient.isPending ? (
        <Loader2 className="mr-1 h-4 w-4 animate-spin" />
      ) : (
        <HeartPulse className="mr-1 h-4 w-4" />
      )}
      Tornar paciente
    </Button>
  );
}
