"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";

import { ROUTES } from "@/lib/constants";

/**
 * Rota de compatibilidade (Sprint 6.4). O Access Gateway redireciona o browser
 * diretamente para o alvo pós-login (default `/`); este callback apenas encaminha
 * para o destino, funcionando como rede de segurança para o fluxo anterior.
 */
export function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const redirect = searchParams.get("redirect");
    router.replace(redirect || ROUTES.DASHBOARD);
  }, [router, searchParams]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
      <p className="mt-4 text-sm text-muted-foreground">
        Autenticação concluída. Redirecionando...
      </p>
    </div>
  );
}
