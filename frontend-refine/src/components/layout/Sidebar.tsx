"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";
import { useSidebar } from "@/store/sidebar";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";
import { NAVIGATION } from "./navigation";
import { SidebarGroup } from "./SidebarGroup";
import * as VisuallyHidden from "@radix-ui/react-visually-hidden";

type SidebarContentProps = {
  collapsed: boolean;
  onNavClick?: () => void;
};

function SidebarContent({ collapsed, onNavClick }: SidebarContentProps) {
  const { logout } = useAuth();

  return (
    <div className="flex h-full flex-col">
      {/* Logo */}
      <div className="flex h-14 items-center border-b border-white/10 px-4">
        {!collapsed && (
          <Link href={ROUTES.DASHBOARD} className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-crm-secondary-purple to-crm-secondary-indigo text-sm font-bold text-white">
              C
            </div>
            <span className="text-lg font-bold text-white">CRM</span>
          </Link>
        )}
        {collapsed && (
          <Link href={ROUTES.DASHBOARD} className="mx-auto flex items-center justify-center">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-crm-secondary-purple to-crm-secondary-indigo text-sm font-bold text-white">
              C
            </div>
          </Link>
        )}
      </div>

      {/* Navigation */}
      <ScrollArea className="flex-1 py-2">
        <TooltipProvider delayDuration={0}>
          <div className="space-y-4 px-2">
            {NAVIGATION.map((group, groupIndex) => (
              <SidebarGroup
                key={groupIndex}
                group={group}
                collapsed={collapsed}
                onNavClick={onNavClick}
              />
            ))}
          </div>
        </TooltipProvider>
      </ScrollArea>

      {/* Footer */}
      <div className="border-t border-white/10 p-2">
        {collapsed ? (
          <TooltipProvider delayDuration={0}>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-9 w-full text-slate-300 hover:bg-purple-500/20 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900"
                  onClick={logout}
                >
                  <LogOut className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent side="right">Sair</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        ) : (
          <Button
            variant="ghost"
            className="w-full justify-start gap-3 text-slate-300 hover:bg-purple-500/20 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900"
            onClick={logout}
          >
            <LogOut className="h-4 w-4" />
            <span className="text-sm">Sair</span>
          </Button>
        )}
      </div>

      {/* Version */}
      {!collapsed && (
        <div className="border-t border-white/10 px-4 py-2 text-center text-xs text-slate-400">
          CRM SaaS v1.0
        </div>
      )}
    </div>
  );
}

export function Sidebar() {
  const { collapsed, toggle, mobileOpen, setMobileOpen } = useSidebar();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const effectiveCollapsed = mounted ? collapsed : false;

  return (
    <>
      {/* Desktop Sidebar */}
      <aside
        className={cn(
          "hidden h-screen flex-col bg-gradient-to-b from-slate-800 to-slate-900 text-white transition-all duration-crm-default ease-in-out lg:flex",
          effectiveCollapsed ? "w-16" : "w-64",
        )}
      >
        <SidebarContent collapsed={effectiveCollapsed} />
        <div className="border-t border-white/10 p-2">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-slate-300 hover:bg-purple-500/20 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900"
            onClick={toggle}
            aria-label={effectiveCollapsed ? "Expandir sidebar" : "Recolher sidebar"}
          >
            {effectiveCollapsed ? (
              <ChevronRight className="h-4 w-4" />
            ) : (
              <ChevronLeft className="h-4 w-4" />
            )}
          </Button>
        </div>
      </aside>

      {/* Mobile Sidebar */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent
          side="left"
          className="w-64 border-none bg-gradient-to-b from-slate-800 to-slate-900 p-0 text-white"
        >
          <VisuallyHidden.Root>
            <SheetTitle>Menu</SheetTitle>
          </VisuallyHidden.Root>
          <SidebarContent collapsed={false} onNavClick={() => setMobileOpen(false)} />
        </SheetContent>
      </Sheet>
    </>
  );
}
