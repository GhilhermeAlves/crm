"use client";

import { ProtectedRoute } from "@/components/layout/ProtectedRoute";

/** Rotas de cobrança fora do DashboardLayout: o layout chama endpoints que respondem 402 quando bloqueada. */
export default function BillingLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <main className="flex min-h-dvh items-center justify-center bg-muted/30 p-4">{children}</main>
    </ProtectedRoute>
  );
}
