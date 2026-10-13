"use client";

import { useParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useContact } from "@/features/masterdata/contacts/hooks/useContacts";
import { PatientFiles } from "@/features/masterdata/contacts/components/PatientFiles";

export default function ContactFilesPage() {
  const params = useParams<{ id: string }>();
  const contactId = params?.id;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: contact, isLoading } = useContact(companyId, contactId);

  if (isLoading || !contact) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando arquivos…
      </div>
    );
  }

  return <PatientFiles contact={contact} />;
}
