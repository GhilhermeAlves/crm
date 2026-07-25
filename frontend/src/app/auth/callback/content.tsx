"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useKeycloak } from "@/providers/KeycloakProvider";
import { Loader2 } from "lucide-react";

export function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { initialized, authenticated } = useKeycloak();
  const [status, setStatus] = useState("Processando autenticação...");

  useEffect(() => {
    if (!initialized) return;

    if (authenticated) {
      const redirect = searchParams.get("redirect") || "/dashboard";
      setStatus("Autenticação concluída. Redirecionando...");
      router.replace(redirect);
    } else {
      setStatus("Falha na autenticação. Redirecionando para o login...");
      setTimeout(() => router.replace("/login"), 1500);
    }
  }, [initialized, authenticated, router, searchParams]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
      <p className="mt-4 text-sm text-muted-foreground">{status}</p>
    </div>
  );
}
