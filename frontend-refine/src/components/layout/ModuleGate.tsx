"use client";

import { type ReactNode } from "react";
import { usePathname } from "next/navigation";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import type { CompanyModuleName } from "@/features/identity/modules/types/modules.types";
import { NAVIGATION } from "./navigation";

const matches = (pathname: string, href: string) =>
  pathname === href || pathname.startsWith(href + "/");

/** Módulo exigido pela rota, a partir dos itens do menu (e seus submenus). */
export function moduleForPath(pathname: string): CompanyModuleName | undefined {
  // Submenu com módulo próprio (ex.: Lista de serviços em Minha Empresa) vence o item.
  for (const group of NAVIGATION) {
    for (const item of group.items) {
      const child = item.children?.find((c) => c.module && matches(pathname, c.href));
      if (child?.module) return child.module;
    }
  }
  for (const group of NAVIGATION) {
    for (const item of group.items) {
      if (!item.module) continue;
      const hrefs = [item.href, ...(item.children?.map((child) => child.href) ?? [])];
      if (hrefs.some((href) => matches(pathname, href))) return item.module;
    }
  }
  return undefined;
}

/** Bloqueia a página de um módulo desligado para a empresa (acesso direto pela URL). */
export function ModuleGate({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { isEnabled } = useActiveCompanyModules();
  const required = moduleForPath(pathname);

  if (required && !isEnabled(required)) {
    return (
      <div className="flex h-full items-center justify-center p-6 text-center">
        <div className="space-y-1">
          <p className="font-medium">Módulo não habilitado para esta empresa</p>
          <p className="text-sm text-muted-foreground">
            Fale com o administrador da plataforma para liberar este módulo.
          </p>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}
