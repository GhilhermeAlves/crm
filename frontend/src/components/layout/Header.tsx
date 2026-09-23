"use client";

import { Menu, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { UserMenu } from "@/components/layout/UserMenu";
import { ThemeToggle } from "@/components/common/ThemeToggle";
import { Breadcrumb } from "@/components/navigation/Breadcrumb";
import { useSidebar } from "@/store/sidebar";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";

export function Header() {
  const { collapsed, toggle, setMobileOpen } = useSidebar();
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

      {/* Desktop sidebar collapse button */}
      <Button
        variant="ghost"
        size="icon"
        className="hidden h-9 w-9 lg:inline-flex"
        onClick={toggle}
        aria-label={collapsed ? "Expandir sidebar" : "Recolher sidebar"}
      >
        {collapsed ? <ChevronRight className="h-5 w-5" /> : <ChevronLeft className="h-5 w-5" />}
      </Button>

      {/* Breadcrumb */}
      <div className="hidden lg:block">
        <Breadcrumb />
      </div>

      {/* Spacer */}
      <div className="flex-1" />

      {/* Notifications */}
      <NotificationBell />

      {/* Theme Toggle */}
      <ThemeToggle />

      {/* User Menu */}
      <UserMenu />
    </header>
  );
}
