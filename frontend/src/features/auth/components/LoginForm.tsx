"use client";

import { useState } from "react";
import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "../hooks/useAuth";

export function LoginForm() {
  const { loginKeycloak } = useAuth();
  const [isRedirecting, setIsRedirecting] = useState(false);

  async function handleKeycloakLogin() {
    setIsRedirecting(true);
    await loginKeycloak();
  }

  return (
    <div className="space-y-4">
      <Button
        type="button"
        className="w-full"
        onClick={handleKeycloakLogin}
        disabled={isRedirecting}
      >
        {isRedirecting ? (
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
        ) : (
          <LogIn className="mr-2 h-4 w-4" />
        )}
        Entrar com Keycloak
      </Button>
      <p className="text-center text-xs text-muted-foreground">
        A autenticação é feita pelo Keycloak (SSO). Você será redirecionado para
        a página de login segura e voltará automaticamente.
      </p>
    </div>
  );
}
