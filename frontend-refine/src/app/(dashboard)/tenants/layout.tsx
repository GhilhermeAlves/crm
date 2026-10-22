"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";

/**
 * Gestão de empresas é exclusiva do dono da plataforma (SUPER_ADMIN). Os demais
 * usuários são levados ao painel; o backend também recusa criar/excluir e só
 * devolve a própria empresa nas leituras.
 */
export default function TenantsLayout({ children }: { children: ReactNode }) {
  const { roles, isLoading } = useAuth();
  const router = useRouter();
  const isSuperAdmin = roles.includes("SUPER_ADMIN");

  useEffect(() => {
    if (!isLoading && !isSuperAdmin) router.replace(ROUTES.DASHBOARD);
  }, [isLoading, isSuperAdmin, router]);

  if (isLoading || !isSuperAdmin) return null;
  return <>{children}</>;
}
