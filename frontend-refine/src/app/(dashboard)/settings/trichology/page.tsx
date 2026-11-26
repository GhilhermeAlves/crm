"use client";

import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { TrichologySettingsPage } from "@/features/clinical/trichology/components/TrichologySettingsPage";

export default function TrichologySettingsRoute() {
  const companyId = useCompanyId();
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">Tricologia</h1>
        <p className="text-sm text-muted-foreground">
          Protocolos de captura, catálogo de achados da IA, marcadores do laboratório e regras do
          copiloto.
        </p>
      </div>
      {companyId ? (
        <TrichologySettingsPage companyId={companyId} />
      ) : (
        <p className="text-sm text-muted-foreground">Carregando…</p>
      )}
    </div>
  );
}
