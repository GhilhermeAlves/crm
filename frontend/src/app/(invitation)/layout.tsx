"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { LoadingScreen } from "@/components/LoadingScreen";

/**
 * Gate da página de aceite de convite (Sprint 8.5).
 * Exige APENAS autenticação — permite usuário com ou sem empresa (o convite pode
 * ser aceito para ingressar numa segunda empresa). Não-autenticado → login
 * preservando a URL com o token (loginKeycloak usa /auth/authorize com redirect).
 */
export default function InvitationGroupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, isLoading, loginKeycloak } = useAuth();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted || isLoading) {
      return;
    }
    if (!isAuthenticated && typeof window !== "undefined") {
      loginKeycloak(`${window.location.pathname}${window.location.search}`);
    }
  }, [mounted, isLoading, isAuthenticated, loginKeycloak]);

  if (!mounted || isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
