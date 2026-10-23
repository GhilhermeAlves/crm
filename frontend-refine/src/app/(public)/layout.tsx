import { type ReactNode } from "react";

/**
 * Grupo de páginas públicas (sem sessão, sem ProtectedRoute). html/body têm overflow
 * hidden (shell do dashboard), então esta área rola por conta própria.
 */
export default function PublicGroupLayout({ children }: { children: ReactNode }) {
  return <div className="h-dvh overflow-y-auto bg-crm-background p-4">{children}</div>;
}
