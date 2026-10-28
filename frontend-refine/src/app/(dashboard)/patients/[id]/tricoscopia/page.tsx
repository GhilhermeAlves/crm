"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { Camera } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { TrichoscopyCamera } from "@/features/clinical/trichoscopy/components/TrichoscopyCamera";
import { TrichoscopyGallery } from "@/features/clinical/trichoscopy/components/TrichoscopyGallery";

export default function PatientTrichoscopyPage() {
  const params = useParams<{ id: string }>();
  const contactId = params.id;
  const companyId = useCompanyId();
  const { can } = useAuthorization();
  const canEdit = can("contact:update");
  const [open, setOpen] = useState(false);

  if (!companyId) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">Tricoscopia</h2>
        {canEdit && (
          <Button onClick={() => setOpen(true)}>
            <Camera className="mr-2 h-4 w-4" /> Nova captura
          </Button>
        )}
      </div>

      <TrichoscopyGallery companyId={companyId} contactId={contactId} canEdit={canEdit} />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[96vh] w-[96vw] max-w-[1600px] overflow-y-auto sm:max-w-[1600px]">
          <DialogHeader>
            <DialogTitle>Captura tricoscópica</DialogTitle>
            <DialogDescription>
              Escolha o tricoscópio, capture a imagem, confira o foco e salve.
            </DialogDescription>
          </DialogHeader>
          {/* Montado só com o diálogo aberto: a câmera desliga ao fechar. */}
          {open && (
            <TrichoscopyCamera
              companyId={companyId}
              contactId={contactId}
              onSaved={() => setOpen(false)}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
