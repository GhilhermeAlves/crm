"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
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

  const navLink = (
    <Link
      href={item.href}
      onClick={onNavClick}
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

  return <div>{navLink}</div>;
}
