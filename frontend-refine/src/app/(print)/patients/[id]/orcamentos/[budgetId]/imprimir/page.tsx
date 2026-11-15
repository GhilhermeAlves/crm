"use client";

import { useEffect } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { BudgetPrintView } from "@/features/clinical/budgets/components/BudgetPrintView";
import { useBudget } from "@/features/clinical/budgets/hooks/useBudgets";
import { parseBlocks } from "@/features/clinical/budgets/utils/blocks";
import { usePatient } from "@/features/clinical/patients/hooks/usePatient";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { useCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { useTenant } from "@/features/identity/tenants/hooks/useTenants";
import { GENDER_OPTIONS } from "@/features/clinical/patients/schemas/patient.schema";

export default function BudgetPrintPage() {
  const params = useParams<{ id: string; budgetId: string }>();
  const searchParams = useSearchParams();
  const companyId = useCompanyId();
  const contactId = params.id;
  const { data: budget } = useBudget(companyId ?? "", contactId, params.budgetId);
  const { data: patient } = usePatient(companyId, contactId);
  const { data: modules } = useCompanyModules(companyId);
  const tenant = useTenant(companyId ?? "");
  // Espera o segmento (coluna de dentes) e a empresa (logo); sem acesso à empresa, imprime sem cabeçalho.
  const tenantSettled = !tenant.isLoading;
  const ready = Boolean(budget && patient && modules && tenantSettled);

  useEffect(() => {
    if (ready && typeof window !== "undefined") window.print();
  }, [ready]);

  if (!budget || !patient || !modules || !tenantSettled) {
    return <p className="p-8 text-sm text-muted-foreground">Carregando...</p>;
  }

  const t = tenant.data;
  const a = t?.address;
  const company = t
    ? {
        name: t.tradingName || t.legalName,
        logoUrl: t.logoUrl,
        addressLine: a
          ? [
              [a.street, a.number].filter(Boolean).join(", "),
              a.neighborhood,
              [a.city, a.state].filter(Boolean).join(" / "),
            ]
              .filter(Boolean)
              .join(" - ")
          : "",
        phone: t.phone,
      }
    : null;

  return (
    <BudgetPrintView
      budget={budget}
      blocks={parseBlocks(searchParams.get("blocos"))}
      patient={{
        name: `${patient.firstName} ${patient.lastName}`.trim(),
        birthDate: patient.birthDate,
        gender: GENDER_OPTIONS.find((o) => o.value === patient.gender)?.label ?? patient.gender,
        mobile: patient.mobile ?? patient.phone,
        cpf: patient.cpf,
        email: patient.email,
      }}
      company={company}
      showTeeth={modules.segment === "DENTAL"}
    />
  );
}
