"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronDown, LayoutGrid } from "lucide-react";
import { cn } from "@/lib/utils";
import { Separator } from "@/components/ui/separator";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import type { NavGroup, NavItem } from "./navigation";
import { SidebarItem } from "./SidebarItem";

type SidebarGroupProps = {
  group: NavGroup;
  collapsed: boolean;
  onNavClick?: () => void;
};

const isPathActive = (pathname: string, href: string) =>
  pathname === href || pathname.startsWith(href + "/");

/** O grupo contém a página aberta (item ou subitem). */
export function groupHasActive(items: NavItem[], pathname: string): boolean {
  return items.some(
    (item) =>
      isPathActive(pathname, item.href) ||
      item.children?.some((child) => isPathActive(pathname, child.href)),
  );
}

export function SidebarGroup({ group, collapsed, onNavClick }: SidebarGroupProps) {
  const { permissions, roles } = useAuth();
  const isCrmAdmin = roles.includes("CRM_ADMIN");
  const { isEnabled: isModuleEnabled } = useActiveCompanyModules();
  const pathname = usePathname();

  const hasPermission = (permission?: string) => {
    if (!permission) return true;
    // UX apenas: sem permissões de negócio carregadas (CurrentUser ainda não
    // está disponível via endpoint público), mantém tudo visível. A autorização
    // final é sempre validada pelo backend. Vira gating real no Sprint 4.
    if (!permissions || permissions.length === 0) return true;
    return permissions.includes(permission);
  };

  const visibleItems = group.items
    .filter((item) => !item.crmAdminOnly || isCrmAdmin)
    .filter((item) => hasPermission(item.permission))
    // Módulos da empresa (V104): enquanto carregam, tudo aparece.
    .filter((item) => !item.module || isModuleEnabled(item.module))
    .map((item) =>
      item.children
        ? {
            ...item,
            children: item.children.filter(
              (child) =>
                hasPermission(child.permission) && (!child.module || isModuleEnabled(child.module)),
            ),
          }
        : item,
    )
    // Item que só agrupa submenus some quando nenhum deles é permitido.
    .filter((item) => !item.children || item.children.length > 0);

  const containsActive = groupHasActive(visibleItems, pathname);
  // O grupo da página aberta já começa expandido.
  const [isExpanded, setIsExpanded] = useState(containsActive);
  useEffect(() => {
    if (containsActive) setIsExpanded(true);
  }, [containsActive]);

  if (visibleItems.length === 0) return null;

  // Barra recolhida: um ícone por grupo, que abre o menu do grupo ao lado.
  if (collapsed && group.title) {
    const GroupIcon = group.icon ?? LayoutGrid;
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            aria-label={`Menu ${group.title}`}
            title={group.title}
            className={cn(
              "flex w-full items-center justify-center rounded-md px-2 py-2 text-slate-200 transition-colors",
              "hover:bg-purple-500/20 hover:text-white",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900",
              containsActive &&
                "bg-gradient-to-r from-crm-secondary-purple to-crm-secondary-indigo text-white shadow-crm-lg",
            )}
          >
            <GroupIcon className="h-4 w-4 shrink-0" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          side="right"
          align="start"
          className="max-h-[80vh] w-60 overflow-y-auto"
        >
          <DropdownMenuLabel>{group.title}</DropdownMenuLabel>
          <DropdownMenuSeparator />
          {visibleItems.map((item) => {
            const Icon = item.icon;
            return (
              <div key={item.href}>
                <DropdownMenuItem asChild>
                  <Link
                    href={item.href}
                    onClick={onNavClick}
                    className={cn(
                      "flex items-center gap-2",
                      isPathActive(pathname, item.href) && !item.children && "font-semibold",
                    )}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    {item.label}
                  </Link>
                </DropdownMenuItem>
                {item.children?.map((child) => (
                  <DropdownMenuItem key={child.href} asChild>
                    <Link
                      href={child.href}
                      onClick={onNavClick}
                      className={cn("pl-8 text-xs", pathname === child.href && "font-semibold")}
                    >
                      {child.label}
                    </Link>
                  </DropdownMenuItem>
                ))}
              </div>
            );
          })}
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  const GroupIcon = group.icon;
  return (
    <div>
      {group.title && (
        <button
          type="button"
          onClick={() => setIsExpanded((prev) => !prev)}
          aria-expanded={isExpanded}
          className="mb-1 flex w-full items-center justify-between gap-1 rounded-md px-3 text-xs font-semibold uppercase tracking-wider text-slate-400 transition-colors hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900"
        >
          <span className="flex items-center gap-2">
            {GroupIcon && <GroupIcon className="h-3.5 w-3.5 shrink-0" />}
            {group.title}
          </span>
          <ChevronDown
            className={cn("h-3.5 w-3.5 shrink-0 transition-transform", !isExpanded && "rotate-180")}
          />
        </button>
      )}
      {!group.title && collapsed && <Separator className="mb-2 bg-white/10" />}
      {(!group.title || isExpanded) && (
        <div className="space-y-0.5">
          {visibleItems.map((item) => (
            <SidebarItem
              key={item.href}
              item={item}
              collapsed={collapsed}
              onNavClick={onNavClick}
            />
          ))}
        </div>
      )}
    </div>
  );
}
