"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { ROUTES } from "@/lib/constants";

type PatientTab = {
  label: string;
  segment: string;
  permission?: string;
};

const TABS: PatientTab[] = [
  { label: "Consultas", segment: "" },
  { label: "Anamnese", segment: "anamnese" },
  { label: "Evoluções", segment: "evolucoes" },
  { label: "Documentos", segment: "documentos" },
  { label: "Financeiro", segment: "financeiro", permission: "finance:read" },
  { label: "Arquivos", segment: "arquivos" },
  { label: "Anotações", segment: "anotacoes" },
];

export function PatientTabs({ contactId }: { contactId: string }) {
  const pathname = usePathname();
  const { can } = useAuthorization();
  const base = `${ROUTES.PATIENTS}/${contactId}`;

  const visibleTabs = TABS.filter((tab) => !tab.permission || can(tab.permission));

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
