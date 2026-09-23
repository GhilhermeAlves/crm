"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { Separator } from "@/components/ui/separator";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { NavGroup } from "./navigation";
import { SidebarItem } from "./SidebarItem";

type SidebarGroupProps = {
  group: NavGroup;
  collapsed: boolean;
  onNavClick?: () => void;
};

export function SidebarGroup({ group, collapsed, onNavClick }: SidebarGroupProps) {
  const { permissions } = useAuth();
  const [collapsedGroup, setCollapsedGroup] = useState(false);
  const isExpanded = !collapsedGroup;

  const toggleGroup = () => {
    setCollapsedGroup((prev) => !prev);
  };

  const hasPermission = (permission?: string) => {
    if (!permission) return true;
    // UX apenas: sem permissões de negócio carregadas (CurrentUser ainda não
    // está disponível via endpoint público), mantém tudo visível. A autorização
    // final é sempre validada pelo backend. Vira gating real no Sprint 4.
    if (!permissions || permissions.length === 0) return true;
    return permissions.includes(permission);
  };

  const visibleItems = group.items.filter((item) => hasPermission(item.permission));

  return (
    <div>
      {group.title && !collapsed && (
        <button
          type="button"
          onClick={toggleGroup}
          aria-expanded={isExpanded}
          className="mb-1 flex w-full items-center justify-between gap-1 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground transition-colors hover:text-foreground"
        >
          <span>{group.title}</span>
          <ChevronDown
            className={cn("h-3.5 w-3.5 shrink-0 transition-transform", !isExpanded && "rotate-180")}
          />
        </button>
      )}
      {group.title && collapsed && <Separator className="mb-2" />}
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
