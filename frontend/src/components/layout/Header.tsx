"use client";

import { Menu, Search, Command } from "lucide-react";
import { Button } from "@/components/ui/button";
import { UserMenu } from "@/components/UserMenu";
import { ThemeToggle } from "@/components/common/ThemeToggle";
import { Breadcrumb } from "@/components/navigation/Breadcrumb";
import { useSidebar } from "@/store/sidebar";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";

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
      <NotificationBell />

      {/* Theme Toggle */}
      <ThemeToggle />

      {/* User Menu */}
      <UserMenu />
    </header>
  );
}
