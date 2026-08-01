"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useKeycloak } from "@/providers/KeycloakProvider";
import { Loader2 } from "lucide-react";

export function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { initialized, authenticated } = useKeycloak();
  const [status, setStatus] = useState("Processando autenticação...");
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) return;
    // Aguarda o KeycloakProvider (único responsável pela inicialização do
    // keycloak-js) concluir o init. Nunca chamamos kc.init aqui de novo —
    // o keycloak-js permite apenas uma inicialização por instância.
    if (!initialized) return;
    processedRef.current = true;

    if (authenticated) {
      const redirect = searchParams.get("redirect") || "/dashboard";
      setStatus("Autenticação concluída. Redirecionando...");
      router.replace(redirect);
    } else {
      const error =
        searchParams.get("error") ||
        new URLSearchParams(window.location.hash.replace("#", "?")).get(
          "error"
        );
      if (error) {
        const desc =
          searchParams.get("error_description") ||
          new URLSearchParams(window.location.hash.replace("#", "?")).get(
            "error_description"
          ) ||
          error;
        setStatus(`Erro de autenticação: ${desc}`);
      } else {
        setStatus(
          "Falha na autenticação. Redirecionando para o login..."
        );
      }
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
