"use client";

import { QueryProvider } from "@/providers/QueryProvider";
import { ThemeProvider } from "@/providers/ThemeProvider";
import { KeycloakProvider } from "@/providers/KeycloakProvider";
import { AuthProvider } from "@/providers/AuthProvider";
import { SidebarProvider } from "@/store/sidebar";
import { Toaster } from "sonner";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      <QueryProvider>
        <KeycloakProvider>
          <AuthProvider>
            <SidebarProvider>
              {children}
              <Toaster richColors position="top-right" />
            </SidebarProvider>
          </AuthProvider>
        </KeycloakProvider>
      </QueryProvider>
    </ThemeProvider>
  );
}
