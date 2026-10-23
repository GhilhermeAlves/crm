"use client";

import Link from "next/link";
import { useRef, useState } from "react";
import { AlertTriangle, Camera, MessageCircle, Pencil } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { formatPhone } from "@/lib/format";
import { ROUTES } from "@/lib/constants";
import { useContactPermissions } from "@/features/masterdata/contacts/hooks/useContacts";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { useCurrentPatientAnamnesis } from "@/features/clinical/anamnesis/hooks/usePatientAnamnesis";
import { useDeletePatientPhoto, usePatientPhoto, useUploadPatientPhoto } from "../hooks/usePatient";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import { toast } from "sonner";

function initialsOf(contact: Contact): string {
  const first = contact.firstName?.trim().charAt(0) ?? "";
  const last = contact.lastName?.trim().charAt(0) ?? "";
  return `${first}${last}`.toUpperCase() || "?";
}

function whatsappHref(mobile: string): string {
  return `https://wa.me/55${mobile.replace(/\D/g, "")}`;
}

const MAX_PHOTO = 5 * 1024 * 1024;
const ALLOWED = ["image/jpeg", "image/png", "image/webp"];

export function PatientHeader({ contact }: { contact: Contact }) {
  const { canUpdate } = useContactPermissions();
  const { can } = useAuthorization();
  const companyId = useCompanyId() ?? "";
  const fullName = [contact.firstName, contact.lastName].filter(Boolean).join(" ");
  const hasMobile = !!contact.mobile && contact.mobile.replace(/\D/g, "").length > 0;
  const { data: photoUrl } = usePatientPhoto(companyId, contact.id, contact.photoStorageId);
  const uploadPhoto = useUploadPatientPhoto(companyId, contact.id);
  const deletePhoto = useDeletePatientPhoto(companyId, contact.id);
  const { data: currentAnamnesis } = useCurrentPatientAnamnesis(companyId, contact.id);
  const fileRef = useRef<HTMLInputElement>(null);
  const [openMenu, setOpenMenu] = useState(false);

  const canEditPhoto = canUpdate || can("contact:update");
  const alerts = currentAnamnesis?.alerts || [];

  const handleFile = (file: File | null) => {
    if (!file) return;
    if (!ALLOWED.includes(file.type)) {
      toast.error("A foto deve ser JPG, PNG ou WEBP de até 5 MB.");
      return;
    }
    if (file.size > MAX_PHOTO) {
      toast.error("A foto deve ser JPG, PNG ou WEBP de até 5 MB.");
      return;
    }
    uploadPhoto.mutate(file);
    setOpenMenu(false);
  };

  return (
    <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-4">
        <Popover open={openMenu} onOpenChange={setOpenMenu}>
          <PopoverTrigger asChild>
            <button
              type="button"
              className="group relative"
              disabled={!canEditPhoto}
              aria-label="Foto do paciente"
            >
              <Avatar className="h-14 w-14">
                {photoUrl ? <AvatarImage src={photoUrl} alt={fullName} /> : null}
                <AvatarFallback className="bg-primary/10 text-lg font-bold text-primary">
                  {initialsOf(contact)}
                </AvatarFallback>
              </Avatar>
              {canEditPhoto && (
                <span className="absolute -bottom-1 -right-1 rounded-full bg-background p-1 shadow">
                  <Camera className="h-3 w-3" />
                </span>
              )}
            </button>
          </PopoverTrigger>
          {canEditPhoto && (
            <PopoverContent className="w-48 p-1">
              <Button
                variant="ghost"
                size="sm"
                className="w-full justify-start"
                onClick={() => fileRef.current?.click()}
              >
                Alterar foto
              </Button>
              {contact.photoStorageId && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="w-full justify-start"
                  onClick={() => {
                    deletePhoto.mutate();
                    setOpenMenu(false);
                  }}
                >
                  Remover foto
                </Button>
              )}
            </PopoverContent>
          )}
        </Popover>
        <input
          ref={fileRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          onChange={(e) => handleFile(e.target.files?.[0] || null)}
        />
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight">{fullName || "Paciente"}</h1>
            {contact.recordNumber && (
              <Badge variant="secondary">Prontuário nº {contact.recordNumber}</Badge>
            )}
            {alerts.length > 0 ? (
              <Popover>
                <PopoverTrigger asChild>
                  <Badge variant="destructive" className="cursor-pointer gap-1">
                    <AlertTriangle className="h-3 w-3" />
                    {alerts.length} alerta(s) na anamnese
                  </Badge>
                </PopoverTrigger>
                <PopoverContent className="w-80">
                  <div className="space-y-2">
                    <p className="text-sm font-medium">Alertas da anamnese</p>
                    <ul className="space-y-1 text-sm">
                      {alerts.map((a, idx) => (
                        <li key={idx} className="text-muted-foreground">
                          {a}
                        </li>
                      ))}
                    </ul>
                    <Button variant="link" size="sm" asChild className="px-0">
                      <Link href={`${ROUTES.PATIENTS}/${contact.id}/anamnese`}>Ver anamnese</Link>
                    </Button>
                  </div>
                </PopoverContent>
              </Popover>
            ) : currentAnamnesis ? null : (
              <Link href={`${ROUTES.PATIENTS}/${contact.id}/anamnese`}>
                <Badge variant="outline">Sem anamnese</Badge>
              </Link>
            )}
          </div>
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
            <Link href={`${ROUTES.PATIENTS}/${contact.id}/edit`}>
              <Pencil className="mr-1 h-4 w-4" /> Editar
            </Link>
          </Button>
        )}
      </div>
    </header>
  );
}
