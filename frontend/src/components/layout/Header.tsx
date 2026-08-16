"use client";

import { Bell, Menu, Search, Command } from "lucide-react";
import { Button } from "@/components/ui/button";
import { UserMenu } from "@/components/UserMenu";
import { ThemeToggle } from "@/components/common/ThemeToggle";
import { Breadcrumb } from "@/components/navigation/Breadcrumb";
import { useSidebar } from "@/store/sidebar";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function Header() {
  const { setMobileOpen } = useSidebar();
  const { user } = useAuth();

  return (
    <header className="flex h-14 items-center gap-4 border-b bg-card px-4 lg:px-6">
      {/* Mobile menu button */}
      <Button
        variant="ghost"
        size="icon"
        className="h-9 w-9 lg:hidden"
        onClick={() => setMobileOpen(true)}
        aria-label="Abrir menu"
      >
        <Menu className="h-5 w-5" />
      </Button>

      {/* Breadcrumb */}
      <div className="hidden lg:block">
        <Breadcrumb />
      </div>

      {/* Spacer */}
      <div className="flex-1" />

      {/* Search (UI only) */}
      <Button variant="outline" size="sm" className="hidden h-9 gap-2 md:flex md:w-64 lg:w-80">
        <Search className="h-4 w-4 text-muted-foreground" />
        <span className="flex-1 text-left text-sm text-muted-foreground">Pesquisar...</span>
        <kbd className="pointer-events-none hidden h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground sm:flex">
          <Command className="h-3 w-3" />K
        </kbd>
      </Button>

      {/* Notifications */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="relative h-9 w-9">
            <Bell className="h-4 w-4" />
            <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-80" align="end">
          <DropdownMenuLabel className="flex items-center justify-between">
            <span>Notificações</span>
            <Button variant="ghost" size="sm" className="h-auto p-0 text-xs text-primary">
              Marcar como lidas
            </Button>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem className="flex flex-col items-start gap-1 py-3">
            <p className="text-sm font-medium">Novo lead atribuído</p>
            <p className="text-xs text-muted-foreground">
              João Silva foi atribuído a você há 5 minutos
            </p>
          </DropdownMenuItem>
          <DropdownMenuItem className="flex flex-col items-start gap-1 py-3">
            <p className="text-sm font-medium">Reunião agendada</p>
            <p className="text-xs text-muted-foreground">Reunião com Empresa X amanhã às 14h</p>
          </DropdownMenuItem>
          <DropdownMenuItem className="flex flex-col items-start gap-1 py-3">
            <p className="text-sm font-medium">Negócio atualizado</p>
            <p className="text-xs text-muted-foreground">Proposta aceita para Empresa Y</p>
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem className="justify-center text-sm text-primary">
            Ver todas as notificações
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Theme Toggle */}
      <ThemeToggle />

      {/* User Menu */}
      <UserMenu />
    </header>
  );
}
