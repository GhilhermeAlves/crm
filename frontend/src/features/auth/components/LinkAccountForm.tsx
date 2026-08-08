"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { AlertCircle, KeyRound, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  getLinkStatus,
  linkAccountWithPassword,
  GatewayLinkError,
} from "@/lib/gateway-auth";

/**
 * Sprint 7.2 (Caso B) — vínculo da conta local.
 *
 * <p>O login Google de um e-mail que já possui conta local (sem `keycloak_sub`)
 * exige a verificação explícita da senha da conta local antes de vincular. Este
 * formulário: consulta o estado do vínculo pendente (exibe o e-mail), recebe a
 * senha e conclui o vínculo via `POST /auth/link` (CSRF cookie-to-header). Em
 * sucesso o gateway seta a sessão real e o browser navega para o redirect
 * original — nenhum token passa pelo browser.
 */
export function LinkAccountForm() {
  const [status, setStatus] = useState<{ pending: boolean; email?: string }>({
    pending: true,
  });
  const [checked, setChecked] = useState(false);
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getLinkStatus().then((result) => {
      if (cancelled) return;
      setStatus(result);
      setChecked(true);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!password) {
      setError("Digite a senha da conta local.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      const result = await linkAccountWithPassword(password);
      window.location.assign(result.redirect);
    } catch (caught) {
      if (caught instanceof GatewayLinkError) {
        setError(messageFor(caught.code));
        if (
          caught.code === "LINK_PENDING_NOT_FOUND" ||
          caught.code === "LINK_NOT_FOUND"
        ) {
          window.location.assign("/login");
          return;
        }
      } else {
        setError("Falha ao vincular a conta. Tente novamente.");
      }
      setPassword("");
      setIsSubmitting(false);
    }
  }

  if (!checked) {
    return (
      <div className="flex justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-crm-primary" />
      </div>
    );
  }

  if (!status.pending) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-crm-text-secondary">
          Não há vínculo pendente nesta sessão.
        </p>
        <Link href="/login" className="block text-center text-sm font-medium text-crm-primary hover:underline">
          Ir para o login
        </Link>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="rounded-lg border border-crm-border bg-crm-background p-3 text-sm">
        <p className="text-crm-text-secondary">
          Sua conta do Google não está vinculada a uma conta do CRM.
        </p>
        <p className="mt-1 text-crm-text">
          Digite a senha da conta local{" "}
          {status.email ? (
            <span className="font-medium text-crm-text">({status.email})</span>
          ) : null}{" "}
          para vincular e continuar.
        </p>
      </div>

      <label className="block space-y-1.5">
        <span className="text-sm font-medium text-crm-text">Senha da conta local</span>
        <Input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••••"
          autoComplete="current-password"
          required
          disabled={isSubmitting}
        />
      </label>

      {error ? (
        <p
          role="alert"
          className="flex items-start gap-2 text-sm text-destructive"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {error}
        </p>
      ) : null}

      <Button type="submit" variant="crm" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <KeyRound className="h-4 w-4" />
        )}
        Vincular e entrar
      </Button>
    </form>
  );
}

function messageFor(code: string): string {
  switch (code) {
    case "INVALID_CREDENTIALS":
      return "Senha inválida. Verifique e tente novamente.";
    case "LINK_PENDING_NOT_FOUND":
      return "O vínculo expirou. Faça o login novamente.";
    case "LINK_NOT_FOUND":
      return "Conta local não encontrada (pode ter sido removida).";
    case "RATE_LIMIT_EXCEEDED":
      return "Muitas tentativas. Aguarde alguns instantes e tente novamente.";
    case "CSRF_INVALID":
      return "Sessão de vínculo inválida. Faça o login novamente.";
    default:
      return "Falha ao vincular a conta. Tente novamente.";
  }
}
