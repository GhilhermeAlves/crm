"use client";

import { PageTitle } from "@/components/common/PageTitle";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { CompanyMailCard } from "@/features/masterdata/company/mail/CompanyMailCard";
import { InviteTemplateCard } from "@/features/masterdata/company/mail/InviteTemplateCard";

export default function EmailSettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="space-y-1">
        <PageTitle>E-mail</PageTitle>
        <p className="text-sm text-muted-foreground">
          Conta de e-mail da clínica e modelo do convite enviado para novas pessoas.
        </p>
      </div>
      {companyId && <CompanyMailCard companyId={companyId} />}
      {companyId && <InviteTemplateCard companyId={companyId} />}
    </div>
  );
}
