"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { LoadingScreen } from "@/components/layout/LoadingScreen";
import { ROUTES, invitationPath } from "@/lib/constants";

/**
 * Link legado (/invitations/accept?token=...). O middleware já redireciona
 * para /convite/{token}; esta página só cobre navegação no cliente.
 */
function LegacyInvitationRedirect() {
  const router = useRouter();
  const token = useSearchParams().get("token");

  useEffect(() => {
    router.replace(token ? invitationPath(token) : ROUTES.DASHBOARD);
  }, [router, token]);

  return <LoadingScreen />;
}

export default function AcceptInvitationPage() {
  return (
    <Suspense fallback={<LoadingScreen />}>
      <LegacyInvitationRedirect />
    </Suspense>
  );
}
