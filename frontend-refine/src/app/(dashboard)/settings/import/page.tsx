"use client";

import { ImportHistory } from "@/features/masterdata/importing/components/ImportHistory";
import { ImportPanel } from "@/features/masterdata/importing/components/ImportPanel";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";

export default function ImportSettingsPage() {
  const companyId = useCompanyId();
  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-semibold">Importar dados</h1>
        <p className="text-sm text-muted-foreground">
          Traga os dados de outro sistema a partir do arquivo .zip de exportação. Importe na ordem:
          pacientes primeiro, depois procedimentos.
        </p>
      </div>
      {companyId && (
        <>
          <ImportPanel companyId={companyId} kind="patients" />
          <ImportPanel companyId={companyId} kind="procedures" />
          <ImportHistory companyId={companyId} />
        </>
      )}
    </div>
  );
}
