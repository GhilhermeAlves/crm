"use client";

import { PatientImportPanel } from "@/features/masterdata/importing/components/PatientImportPanel";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";

export default function ImportSettingsPage() {
  const companyId = useCompanyId();
  return (
    <div className="space-y-4 p-6">
      <div>
        <h1 className="text-2xl font-semibold">Importar dados</h1>
        <p className="text-sm text-muted-foreground">
          Traga os pacientes de outro sistema a partir do arquivo .zip de exportação.
        </p>
      </div>
      {companyId && <PatientImportPanel companyId={companyId} />}
    </div>
  );
}
