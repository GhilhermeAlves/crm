"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useKeycloak } from "@/providers/KeycloakProvider";
import { TokenManager } from "@/store/token-manager";
import { LoadingScreen } from "./LoadingScreen";
import { ROUTES } from "@/lib/constants";

type ProtectedRouteProps = {
  children: React.ReactNode;
};

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const keycloakCtx = useKeycloak();
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  const hasKcToken = !!TokenManager.getAccessToken();
  const waitingForKeycloak = hasKcToken && !keycloakCtx.initialized;
  const isEffectiveAuthenticated = isAuthenticated || (hasKcToken && keycloakCtx.authenticated);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted && !waitingForKeycloak && !isLoading && !isEffectiveAuthenticated) {
      router.push(ROUTES.LOGIN);
    }
  }, [mounted, waitingForKeycloak, isEffectiveAuthenticated, isLoading, router]);

  if (!mounted || waitingForKeycloak || (isLoading && !keycloakCtx.authenticated)) {
    return <LoadingScreen />;
  }

  if (!isEffectiveAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
