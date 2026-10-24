"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { useCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { ROUTES } from "@/lib/constants";

type PatientTab = {
  label: string;
  segment: string;
  permission?: string;
  trichologyOnly?: boolean;
};

const TABS: PatientTab[] = [
  { label: "Consultas", segment: "" },
  { label: "Ficha clínica", segment: "ficha-clinica" },
  { label: "Orçamentos", segment: "orcamentos" },
  { label: "Anamnese", segment: "anamnese" },
  { label: "Evoluções", segment: "evolucoes" },
  { label: "Documentos", segment: "documentos" },
  { label: "Tricoscopia", segment: "tricoscopia", trichologyOnly: true },
  { label: "Financeiro", segment: "financeiro", permission: "finance:read" },
  { label: "Arquivos", segment: "arquivos" },
  { label: "Anotações", segment: "anotacoes" },
];

export function PatientTabs({ contactId }: { contactId: string }) {
  const pathname = usePathname();
  const { can } = useAuthorization();
  const companyId = useCompanyId();
  const { data: modules } = useCompanyModules(companyId);
  const isTrichology = modules?.segment === "TRICHOLOGY";
  const base = `${ROUTES.PATIENTS}/${contactId}`;

  const visibleTabs = TABS.filter((tab) => {
    if (tab.permission && !can(tab.permission)) return false;
    if (tab.trichologyOnly && !isTrichology) return false;
    return true;
  });

  return (
    <nav aria-label="Seções do paciente" className="overflow-x-auto border-b">
      <div className="flex min-w-max gap-1">
        {visibleTabs.map((tab) => {
          const href = tab.segment ? `${base}/${tab.segment}` : base;
          const isActive = tab.segment
            ? pathname === href || pathname.startsWith(`${href}/`)
            : pathname === base;
          return (
            <Link
              key={tab.segment || "consultas"}
              href={href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "border-b-2 border-transparent px-4 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground",
                isActive && "border-primary text-foreground",
              )}
            >
              {tab.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
