"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { PageTitle } from "@/components/common/PageTitle";
import { ErrorCard } from "@/components/common/ErrorCard";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { CompanyModulesCard } from "@/features/identity/modules/components/CompanyModulesCard";
import { useTenants } from "@/features/identity/tenants/hooks/useTenants";
import { ROUTES } from "@/lib/constants";

/** Módulos de cada empresa (só o dono da plataforma liga e desliga). */
export default function CompanyModulesPage() {
  const router = useRouter();
  const params = useSearchParams();
  const { roles } = useAuth();
  const isPlatformAdmin = roles.includes("SUPER_ADMIN");
  const { data: tenants = [], isLoading, error } = useTenants();
  const [companyId, setCompanyId] = useState<string | null>(params.get("company"));

  useEffect(() => {
    if (!companyId && tenants.length > 0) setCompanyId(tenants[0].id);
  }, [companyId, tenants]);

  function choose(id: string) {
    setCompanyId(id);
    router.replace(`${ROUTES.COMPANY_MODULES}?company=${id}`);
  }

  if (!isPlatformAdmin) {
    return (
      <div className="space-y-6">
        <PageTitle>Módulos</PageTitle>
        <ErrorCard message="Somente o dono da plataforma gerencia os módulos das empresas." />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle>Módulos</PageTitle>
      {error && <ErrorCard message="Não foi possível carregar as empresas." />}
      <div className="max-w-sm space-y-2">
        <Label htmlFor="modules-company">Empresa</Label>
        <Select value={companyId ?? undefined} onValueChange={choose} disabled={isLoading}>
          <SelectTrigger id="modules-company" aria-label="Empresa">
            <SelectValue placeholder={isLoading ? "Carregando…" : "Escolha a empresa"} />
          </SelectTrigger>
          <SelectContent>
            {tenants.map((t) => (
              <SelectItem key={t.id} value={t.id}>
                {t.tradingName}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {companyId && <CompanyModulesCard key={companyId} companyId={companyId} />}
    </div>
  );
}
