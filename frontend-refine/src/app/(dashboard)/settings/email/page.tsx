"use client";

import { PageTitle } from "@/components/common/PageTitle";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { CompanyMailCard } from "@/features/masterdata/company/mail/CompanyMailCard";

export default function EmailSettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="space-y-1">
        <PageTitle>E-mail</PageTitle>
        <p className="text-sm text-muted-foreground">
          Conta de e-mail da clínica para enviar formulários aos pacientes.
        </p>
      </div>
      {companyId && <CompanyMailCard companyId={companyId} />}
    </div>
  );
}
