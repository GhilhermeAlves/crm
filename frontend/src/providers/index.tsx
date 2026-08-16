"use client";

import { QueryProvider } from "@/providers/QueryProvider";
import { ThemeProvider } from "@/providers/ThemeProvider";
import { AuthProvider } from "@/providers/AuthProvider";
import { SidebarProvider } from "@/store/sidebar";
import { Toaster } from "sonner";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryProvider>
        <AuthProvider>
          <SidebarProvider>
            {children}
            <Toaster richColors position="top-right" />
          </SidebarProvider>
        </AuthProvider>
      </QueryProvider>
    </ThemeProvider>
  );
}
