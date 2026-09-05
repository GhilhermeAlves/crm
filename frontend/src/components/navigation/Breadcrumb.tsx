"use client";

import { useMemo } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronRight, Home } from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";

const routeLabels: Record<string, string> = {
  crm: "CRM",
  dashboard: "Dashboard",
  tenants: "Empresas",
  users: "Usuários",
  profile: "Meu Perfil",
  new: "Nova",
  edit: "Editar",
  leads: "Leads",
  contacts: "Contatos",
  pipeline: "Pipeline",
  chat: "Chat",
  campaigns: "Campanhas",
  reports: "Relatórios",
  settings: "Segurança",
  roles: "Perfis",
};

type BreadcrumbProps = {
  className?: string;
};

export function Breadcrumb({ className }: BreadcrumbProps) {
  const pathname = usePathname();

  const segments = useMemo(() => {
    const parts = pathname.split("/").filter(Boolean);
    return parts
      .map((part, index) => {
        const href = "/" + parts.slice(0, index + 1).join("/");
        const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(part);
        if (isUuid) return null;
        const label = routeLabels[part] || part.charAt(0).toUpperCase() + part.slice(1);
        return { label, href, isLast: index === parts.length - 1 };
      })
      .filter(Boolean) as { label: string; href: string; isLast: boolean }[];
  }, [pathname]);

  if (
    segments.length <= 1 &&
    (segments[0]?.label === "Dashboard" || segments[0]?.label === "CRM")
  ) {
    return null;
  }

  return (
    <nav aria-label="Breadcrumb" className={cn("flex items-center text-sm", className)}>
      <ol className="flex items-center gap-1">
        <li>
          <Link
            href={ROUTES.DASHBOARD}
            className="flex items-center gap-1 text-muted-foreground transition-colors hover:text-foreground"
          >
            <Home className="h-3.5 w-3.5" />
          </Link>
        </li>
        {segments.map((segment) => (
          <li key={segment.href} className="flex items-center gap-1">
            <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
            {segment.isLast ? (
              <span className="font-medium text-foreground">{segment.label}</span>
            ) : (
              <Link
                href={segment.href}
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                {segment.label}
              </Link>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}
