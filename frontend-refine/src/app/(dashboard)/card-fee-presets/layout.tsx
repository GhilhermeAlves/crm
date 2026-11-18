"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";

/** Modelos de taxa são globais: só o dono da plataforma (SUPER_ADMIN) gerencia. */
export default function CardFeePresetsLayout({ children }: { children: ReactNode }) {
  const { roles, isLoading } = useAuth();
  const router = useRouter();
  const isPlatformAdmin = roles.includes("SUPER_ADMIN");

  useEffect(() => {
    if (!isLoading && !isPlatformAdmin) router.replace(ROUTES.DASHBOARD);
  }, [isLoading, isPlatformAdmin, router]);

  if (isLoading || !isPlatformAdmin) return null;
  return <>{children}</>;
}
