"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { loginWithGateway } from "@/lib/gateway-auth";

/**
 * Ponto de entrada do login. Não há formulário aqui: a única tela de login é a
 * do Keycloak (tema `crm-login`, com o visual da landing). Esta página só
 * inicia o fluxo OIDC, preservando o `?redirect=`, e mostra um estado de
 * carregamento escuro para a transição landing → Keycloak não piscar.
 */
function RedirectToKeycloak() {
  const searchParams = useSearchParams();
  const redirect = searchParams.get("redirect") ?? undefined;

  useEffect(() => {
    loginWithGateway(redirect);
  }, [redirect]);

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-3 bg-black text-white">
      <Loader2 className="h-5 w-5 animate-spin text-white/70" aria-hidden="true" />
      <p role="status" className="text-sm text-white/75">
        Redirecionando para o login seguro…
      </p>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <RedirectToKeycloak />
    </Suspense>
  );
}
