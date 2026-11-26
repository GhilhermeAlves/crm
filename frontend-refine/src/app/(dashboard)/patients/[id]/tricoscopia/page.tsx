"use client";

import { useState } from "react";
import { useParams, usePathname, useRouter, useSearchParams } from "next/navigation";
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
import { useCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { TrichoscopyCamera } from "@/features/clinical/trichoscopy/components/TrichoscopyCamera";
import { TrichoscopyGallery } from "@/features/clinical/trichoscopy/components/TrichoscopyGallery";
import { AssessmentList } from "@/features/clinical/trichology/components/AssessmentList";
import { AssessmentWorkspace } from "@/features/clinical/trichology/components/AssessmentWorkspace";

export default function PatientTrichoscopyPage() {
  const params = useParams<{ id: string }>();
  const contactId = params.id;
  const companyId = useCompanyId();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { can } = useAuthorization();
  const { data: modules } = useCompanyModules(companyId);
  const canEdit = can("contact:update");
  const examsEnabled =
    can("trichology:read") && Boolean(modules?.enabledModules.includes("TRICHOLOGY"));
  const [open, setOpen] = useState(false);
  const assessmentId = searchParams.get("exame");

  const openAssessment = (id: string | null) =>
    router.replace(id ? `${pathname}?exame=${id}` : pathname, { scroll: false });

  if (!companyId) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  if (examsEnabled && assessmentId) {
    return (
      <AssessmentWorkspace
        companyId={companyId}
        contactId={contactId}
        assessmentId={assessmentId}
        onBack={() => openAssessment(null)}
        onOpenAssessment={(id) => openAssessment(id)}
      />
    );
  }

  return (
    <div className="space-y-8">
      {examsEnabled && (
        <AssessmentList
          companyId={companyId}
          contactId={contactId}
          canWrite={can("trichology:write")}
          onOpen={(id) => openAssessment(id)}
        />
      )}

      <section className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-semibold">
            {examsEnabled ? "Galeria de tricoscopia" : "Tricoscopia"}
          </h2>
          {canEdit && (
            <Button variant={examsEnabled ? "outline" : "default"} onClick={() => setOpen(true)}>
              <Camera className="mr-2 h-4 w-4" /> Nova captura avulsa
            </Button>
          )}
        </div>

        <TrichoscopyGallery companyId={companyId} contactId={contactId} canEdit={canEdit} />
      </section>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="flex h-[96dvh] w-[96vw] max-w-[1600px] flex-col overflow-y-auto sm:max-w-[1600px]">
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
