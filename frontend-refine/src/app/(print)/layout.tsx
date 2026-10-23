"use client";

import { ProtectedRoute } from "@/components/layout/ProtectedRoute";

/** Layout mínimo para páginas de impressão: sem sidebar/header, fundo claro e rolagem natural. */
export default function PrintLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-white text-black">{children}</div>
    </ProtectedRoute>
  );
}
