"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { LoadingScreen } from "@/components/LoadingScreen";
import { ROUTES } from "@/lib/constants";

/**
 * Gate do onboarding (Sprint 8.3):
 *  - não autenticado → login (preservando o destino)
 *  - autenticado COM empresa → já onboarded, vai ao dashboard
 *  - autenticado SEM empresa (company_id null) → renderiza o onboarding
 */
export default function OnboardingGroupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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
      router.push(
        `${ROUTES.LOGIN}?redirect=${encodeURIComponent("/onboarding")}`,
      );
    } else if (user?.companyId) {
      router.push(ROUTES.DASHBOARD);
    }
  }, [mounted, isLoading, isAuthenticated, user?.companyId, router]);

  if (!mounted || isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated || user?.companyId) {
    return null;
  }

  return <>{children}</>;
}
