"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ChevronRight, Loader2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useContact } from "@/features/masterdata/contacts/hooks/useContacts";
import { PatientHeader } from "@/features/masterdata/contacts/components/PatientHeader";
import { PatientTabs } from "@/features/masterdata/contacts/components/PatientTabs";
import { ROUTES } from "@/lib/constants";

export default function ContactRecordLayout({ children }: { children: React.ReactNode }) {
  const params = useParams<{ id: string }>();
  const contactId = params?.id;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: contact, isLoading, isError } = useContact(companyId, contactId);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando paciente…
      </div>
    );
  }

  if (isError || !contact) {
    return (
      <p className="py-20 text-center text-sm text-destructive">
        Não foi possível carregar este paciente.
      </p>
    );
  }

  const fullName = [contact.firstName, contact.lastName].filter(Boolean).join(" ");

  return (
    <div className="space-y-6">
      <nav aria-label="Trilha de navegação" className="flex items-center gap-1 text-sm">
        <Link
          href={ROUTES.CONTACTS}
          className="text-muted-foreground transition-colors hover:text-foreground"
        >
          Contatos
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-medium text-foreground">{fullName || "Paciente"}</span>
      </nav>

      <PatientHeader contact={contact} />
      <PatientTabs contactId={contact.id} />

      <div>{children}</div>
    </div>
  );
}
