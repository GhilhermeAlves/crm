"use client";

import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { loginWithGateway } from "@/lib/gateway-auth";

/**
 * Botão de login com e-mail/senha via Keycloak (fluxo OIDC).
 * Redireciona para /auth/authorize sem kc_idp_hint → Keycloak mostra
 * o formulário padrão de email/senha (sem Identity Provider específico).
 * Sprint 7.0: interface unificada, fluxo OIDC sem provedor externo.
 */
export function LoginFormCredentials() {
  const handleClick = () => {
    loginWithGateway();
  };

  return (
    <div className="space-y-4">
      <Button onClick={handleClick} variant="crm" className="w-full">
        <LogIn className="h-4 w-4" />
        Entrar com e-mail e senha
      </Button>

      <p className="text-center text-xs text-crm-text-secondary">
        Login seguro — os dados ficam entre você e nossos servidores.
      </p>
    </div>
  );
}
