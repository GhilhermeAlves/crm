"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "../hooks/useAuth";

export function LoginForm() {
  const { loginKeycloak } = useAuth();
  const searchParams = useSearchParams();
  const [isRedirecting, setIsRedirecting] = useState(false);

  async function handleLogin() {
    setIsRedirecting(true);
    const redirect = searchParams.get("redirect") || undefined;
    await loginKeycloak(redirect);
  }

  return (
    <div className="space-y-4">
      <Button
        type="button"
        variant="crm"
        className="w-full"
        onClick={handleLogin}
        disabled={isRedirecting}
      >
        {isRedirecting ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <LogIn className="h-4 w-4" />
        )}
        Entrar com e-mail e senha
      </Button>
      <p className="text-center text-xs text-crm-text-secondary">
        Você será redirecionado para o login seguro e voltará automaticamente.
      </p>
    </div>
  );
}
