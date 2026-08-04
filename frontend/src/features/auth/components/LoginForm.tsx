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
        className="w-full"
        onClick={handleLogin}
        disabled={isRedirecting}
      >
        {isRedirecting ? (
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
        ) : (
          <LogIn className="mr-2 h-4 w-4" />
        )}
        Entrar
      </Button>
      <p className="text-center text-xs text-muted-foreground">
        Você será redirecionado para o login seguro e voltará automaticamente.
      </p>
    </div>
  );
}
