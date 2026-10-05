"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import type { NavItem } from "./navigation";

type SidebarItemProps = {
  item: NavItem;
  collapsed: boolean;
  onNavClick?: () => void;
};

export function SidebarItem({ item, collapsed, onNavClick }: SidebarItemProps) {
  const pathname = usePathname();
  const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
  const Icon = item.icon;
  const hasChildren = !!item.children?.length;
  const [submenuOpen, setSubmenuOpen] = useState(isActive);

  useEffect(() => {
    if (isActive) setSubmenuOpen(true);
  }, [isActive]);

  // Destaca o subitem de caminho mais específico (ex.: /anamnesis/123 → "Modelos de anamnese")
  const activeChildHref = item.children
    ?.filter((c) => pathname === c.href || pathname.startsWith(c.href + "/"))
    .sort((a, b) => b.href.length - a.href.length)[0]?.href;

  const handleClick =(e: React.MouseEvent) => {
    if (hasChildren && isActive) {
      // Já está na seção: o clique só abre/fecha o submenu
      e.preventDefault();
      setSubmenuOpen((prev) => !prev);
      return;
    }
    onNavClick?.();
  };

  const navLink = (
    <Link
      href={item.href}
      onClick={handleClick}
      aria-expanded={hasChildren ? submenuOpen : undefined}
      className={cn(
        "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-200 transition-all duration-crm-default ease-in-out",
        "hover:translate-x-1 hover:bg-purple-500/20 hover:text-white",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900",
        isActive &&
          "bg-gradient-to-r from-crm-secondary-purple to-crm-secondary-indigo text-white shadow-crm-lg",
        collapsed && "justify-center px-2",
      )}
    >
      <Icon className="h-4 w-4 shrink-0" />
      {!collapsed && <span className="flex-1">{item.label}</span>}
      {!collapsed && hasChildren && (
        <ChevronDown
          className={cn("h-3.5 w-3.5 shrink-0 transition-transform", !submenuOpen && "-rotate-90")}
        />
      )}
      {!collapsed && item.badge && (
        <span
          className={cn(
            "rounded-full px-2 py-0.5 text-xs font-medium",
            isActive ? "bg-white/20 text-white" : "bg-white/10 text-slate-200",
          )}
        >
          {item.badge}
        </span>
      )}
    </Link>
  );

  if (collapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>{navLink}</TooltipTrigger>
        <TooltipContent side="right">{item.label}</TooltipContent>
      </Tooltip>
    );
  }

  if (!item.children?.length || !submenuOpen) return <div>{navLink}</div>;

  return (
    <div>
      {navLink}
      <div className="ml-5 mt-0.5 space-y-0.5 border-l border-white/10 pl-3">
        {item.children.map((child) => {
          const childActive = child.href === activeChildHref;
          return (
            <Link
              key={child.href}
              href={child.href}
              onClick={onNavClick}
              className={cn(
                "block rounded-md px-2 py-1.5 text-xs text-slate-300 transition-colors hover:bg-purple-500/20 hover:text-white",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white",
                childActive && "bg-white/10 font-medium text-white",
              )}
            >
              {child.label}
            </Link>
          );
        })}
      </div>
    </div>
  );
}
