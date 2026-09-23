"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { LoadingScreen } from "./LoadingScreen";
import { ROUTES } from "@/lib/constants";

type ProtectedRouteProps = {
  children: React.ReactNode;
};

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted || isLoading) {
      return;
    }
    if (!isAuthenticated) {
      router.push(ROUTES.LOGIN);
    } else if (!user?.companyId) {
      // Sprint 8.3: usuário sem empresa — onboarding pendente. Protege os módulos
      // do CRM até ele criar a primeira empresa.
      router.push(ROUTES.ONBOARDING);
    }
  }, [mounted, isLoading, isAuthenticated, user?.companyId, router]);

  if (!mounted || isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated || !user?.companyId) {
    return null;
  }

  return <>{children}</>;
}
