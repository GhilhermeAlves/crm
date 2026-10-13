"use client";

import Link from "next/link";
import { MessageCircle, Pencil } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatPhone } from "@/lib/format";
import { useContactPermissions } from "../hooks/useContacts";
import type { Contact } from "../types/contact.types";

function initialsOf(contact: Contact): string {
  const first = contact.firstName?.trim().charAt(0) ?? "";
  const last = contact.lastName?.trim().charAt(0) ?? "";
  return `${first}${last}`.toUpperCase() || "?";
}

function whatsappHref(mobile: string): string {
  return `https://wa.me/55${mobile.replace(/\D/g, "")}`;
}

export function PatientHeader({ contact }: { contact: Contact }) {
  const { canUpdate } = useContactPermissions();
  const fullName = [contact.firstName, contact.lastName].filter(Boolean).join(" ");
  const hasMobile = !!contact.mobile && contact.mobile.replace(/\D/g, "").length > 0;

  return (
    <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-4">
        <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-primary/10 text-lg font-bold text-primary">
          {initialsOf(contact)}
        </span>
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight">{fullName || "Paciente"}</h1>
            {contact.recordNumber && (
              <Badge variant="secondary">Prontuário nº {contact.recordNumber}</Badge>
            )}
          </div>
          {hasMobile && (
            <p className="text-sm text-muted-foreground">{formatPhone(contact.mobile as string)}</p>
          )}
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {hasMobile && (
          <Button size="sm" variant="outline" asChild>
            <a href={whatsappHref(contact.mobile as string)} target="_blank" rel="noreferrer">
              <MessageCircle className="mr-1 h-4 w-4" /> WhatsApp
            </a>
          </Button>
        )}
        {canUpdate && (
          <Button size="sm" asChild>
            <Link href={`/contacts/${contact.id}/edit`}>
              <Pencil className="mr-1 h-4 w-4" /> Editar
            </Link>
          </Button>
        )}
      </div>
    </header>
  );
}
