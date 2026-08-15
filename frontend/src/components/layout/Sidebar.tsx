"use client";

import { useCallback, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Users,
  Contact,
  GitBranch,
  MessageSquare,
  Megaphone,
  BarChart3,
  Settings,
  Building2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  LogOut,
  Shield,
  MailPlus,
  KeyRound,
  ClipboardList,
  HardDrive,
  Workflow as WorkflowIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";
import { useSidebar } from "@/store/sidebar";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";
import * as VisuallyHidden from "@radix-ui/react-visually-hidden";
import { useState, useEffect } from "react";

type NavItem = {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
  permission?: string;
};

type NavGroup = {
  title?: string;
  items: NavItem[];
};

const navGroups: NavGroup[] = [
  {
    items: [
      { label: "Dashboard", href: ROUTES.DASHBOARD, icon: LayoutDashboard, permission: "dashboard:view" },
    ],
  },
  {
    title: "Administração",
    items: [
      { label: "Empresas", href: ROUTES.TENANTS, icon: Building2, permission: "company:view" },
          { label: "Usuários", href: ROUTES.USERS, icon: Users, permission: "user:read" },
          { label: "Membros", href: ROUTES.MEMBERS, icon: Users, permission: "membership:view" },
          { label: "Convites", href: ROUTES.INVITATIONS, icon: MailPlus, permission: "membership:view" },
      { label: "Roles", href: ROUTES.ROLES, icon: Shield, permission: "role:read" },
      { label: "Permissões", href: ROUTES.PERMISSIONS, icon: KeyRound, permission: "role:read" },
      { label: "Arquivos", href: ROUTES.STORAGE, icon: HardDrive },
    ],
  },
  {
    title: "CRM",
    items: [
      { label: "Leads", href: ROUTES.LEADS, icon: Users, permission: "lead:read" },
      { label: "Contatos", href: ROUTES.CONTACTS, icon: Contact, permission: "contact:read" },
      { label: "Pipeline", href: ROUTES.PIPELINE, icon: GitBranch, permission: "pipeline:view" },
      { label: "Tarefas", href: ROUTES.TASKS, icon: ClipboardList, permission: "task:read" },
      { label: "Timeline", href: ROUTES.ACTIVITIES, icon: MailPlus, permission: "activity:read" },
      { label: "Workflows", href: ROUTES.WORKFLOWS, icon: WorkflowIcon, permission: "workflow:read" },
    ],
  },
  {
    title: "Comunicação",
    items: [
      { label: "Inbox", href: ROUTES.INBOX, icon: MessageSquare, permission: "omnichannel:read" },
      { label: "Canais", href: ROUTES.CHANNELS, icon: Megaphone, permission: "omnichannel:read" },
    ],
  },
  {
    title: "Análise",
    items: [
      { label: "Relatórios", href: ROUTES.REPORTS, icon: BarChart3 },
    ],
  },
  {
    title: "Sistema",
    items: [
      { label: "Auditoria", href: ROUTES.AUDIT, icon: ClipboardList, permission: "audit:read" },
      { label: "Configurações", href: ROUTES.SETTINGS, icon: Settings },
      { label: "Usuários", href: ROUTES.SETTINGS_USERS, icon: Users, permission: "membership:view" },
      { label: "Perfis & permissões", href: ROUTES.SETTINGS_ROLES, icon: Shield, permission: "role:read" },
    ],
  },
];

type SidebarContentProps = {
  collapsed: boolean;
  onNavClick?: () => void;
};

function SidebarContent({ collapsed, onNavClick }: SidebarContentProps) {
  const pathname = usePathname();
  const { logout, permissions } = useAuth();

  const hasPermission = (permission?: string) => {
    if (!permission) return true;
    // UX apenas: sem permissões de negócio carregadas (CurrentUser ainda não
    // está disponível via endpoint público), mantém tudo visível. A autorização
    // final é sempre validada pelo backend. Vira gating real no Sprint 4.
    if (!permissions || permissions.length === 0) return true;
    return permissions.includes(permission);
  };

  return (
    <div className="flex h-full flex-col">
      {/* Logo */}
      <div className="flex h-14 items-center border-b px-4">
        {!collapsed && (
          <Link href={ROUTES.DASHBOARD} className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-sm font-bold">
              C
            </div>
            <span className="text-lg font-bold">CRM</span>
          </Link>
        )}
        {collapsed && (
          <Link href={ROUTES.DASHBOARD} className="mx-auto flex items-center justify-center">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-sm font-bold">
              C
            </div>
          </Link>
        )}
      </div>

      {/* Navigation */}
      <ScrollArea className="flex-1 py-2">
        <TooltipProvider delayDuration={0}>
          <div className="space-y-4 px-2">
            {navGroups.map((group, groupIndex) => (
              <div key={groupIndex}>
                {group.title && !collapsed && (
                  <div className="mb-1 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {group.title}
                  </div>
                )}
                {group.title && collapsed && <Separator className="mb-2" />}
                <div className="space-y-0.5">
                  {group.items.filter((item) => hasPermission(item.permission)).map((item) => {
                    const Icon = item.icon;
                    const isActive = pathname === item.href || pathname.startsWith(item.href + "/");

                    const navLink = (
                      <Link
                        href={item.href}
                        onClick={onNavClick}
                        className={cn(
                          "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                          "hover:bg-accent hover:text-accent-foreground",
                          isActive && "bg-accent text-accent-foreground",
                          collapsed && "justify-center px-2",
                        )}
                      >
                        <Icon className="h-4 w-4 shrink-0" />
                        {!collapsed && <span className="flex-1">{item.label}</span>}
                        {!collapsed && item.badge && (
                          <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                            {item.badge}
                          </span>
                        )}
                      </Link>
                    );

                    if (collapsed) {
                      return (
                        <Tooltip key={item.href}>
                          <TooltipTrigger asChild>{navLink}</TooltipTrigger>
                          <TooltipContent side="right">{item.label}</TooltipContent>
                        </Tooltip>
                      );
                    }

                    return <div key={item.href}>{navLink}</div>;
                  })}
                </div>
              </div>
            ))}
          </div>
        </TooltipProvider>
      </ScrollArea>

      {/* Footer */}
      <div className="border-t p-2">
        {collapsed ? (
          <TooltipProvider delayDuration={0}>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="w-full h-9"
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
            className="w-full justify-start gap-3 text-muted-foreground"
            onClick={logout}
          >
            <LogOut className="h-4 w-4" />
            <span className="text-sm">Sair</span>
          </Button>
        )}
      </div>

      {/* Version */}
      {!collapsed && (
        <div className="border-t px-4 py-2 text-center text-xs text-muted-foreground">
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
          "hidden lg:flex h-screen flex-col border-r bg-card transition-all duration-300 ease-in-out",
          effectiveCollapsed ? "w-16" : "w-64",
        )}
      >
        <SidebarContent collapsed={effectiveCollapsed} />
        <div className="border-t p-2">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
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
        <SheetContent side="left" className="w-64 p-0">
          <VisuallyHidden.Root>
            <SheetTitle>Menu</SheetTitle>
          </VisuallyHidden.Root>
          <SidebarContent collapsed={false} onNavClick={() => setMobileOpen(false)} />
        </SheetContent>
      </Sheet>
    </>
  );
}
