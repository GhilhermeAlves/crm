"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { getKeycloakInstance } from "@/lib/keycloak";
import { TokenManager } from "@/store/token-manager";
import { Loader2 } from "lucide-react";

export function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState("Processando autenticação...");
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) return;
    processedRef.current = true;

    const process = async () => {
      try {
        const kc = getKeycloakInstance();

        await kc.init({
          onLoad: "check-sso",
          silentCheckSsoRedirectUri:
            window.location.origin + "/silent-check-sso.html",
          pkceMethod: "S256",
          checkLoginIframe: false,
        });

        if (kc.authenticated) {
          TokenManager.setTokens(kc.token || "", kc.refreshToken || null);

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
      } catch {
        setStatus("Erro ao processar autenticação. Redirecionando...");
        setTimeout(() => router.replace("/login"), 2000);
      }
    };

    process();
  }, [router, searchParams]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
      <p className="mt-4 text-sm text-muted-foreground">{status}</p>
    </div>
  );
}
