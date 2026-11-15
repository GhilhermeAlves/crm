"use client";

import { QueryProvider } from "@/providers/QueryProvider";
import { ThemeProvider } from "@/providers/ThemeProvider";
import { AuthProvider } from "@/providers/AuthProvider";
import { rehydrateSidebarStore } from "@/stores/sidebar.store";
import { Toaster } from "sonner";
import { useEffect } from "react";

export function Providers({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    rehydrateSidebarStore();
  }, []);

  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryProvider>
        <AuthProvider>
          {children}
          <Toaster
            richColors
            position="top-right"
            closeButton
            duration={5000}
            toastOptions={{
              className: "sonner-toast-with-progress",
            }}
          />
        </AuthProvider>
      </QueryProvider>
    </ThemeProvider>
  );
}
