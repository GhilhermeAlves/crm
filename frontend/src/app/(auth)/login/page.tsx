"use client";

import { Suspense } from "react";
import Link from "next/link";
import { LoginBrand } from "@/components/brand/LoginBrand";
import { LoginForm } from "@/features/auth/components/LoginForm";
import { ProviderList } from "@/features/auth/components/ProviderList";

/**
 * Tela de login (Sprint 7.0). Apresenta o catálogo de provedores de identidade
 * (Google, Microsoft, Apple, Telefone — habilitados conforme o servidor) e
 * mantém o acesso clássico por e-mail/senha via Keycloak ("Entrar"), que
 * redireciona ao login seguro do Access Gateway. Nenhum token de provedor
 * externo transita pelo browser.
 */
export default function LoginPage() {
  return (
    <Suspense>
      <div className="space-y-6">
        <LoginBrand variant="mobile" wordmark="CRM" className="pt-2" />
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-crm-text">Entrar</h1>
          <p className="text-sm text-crm-text-secondary">Acesse sua conta para continuar</p>
        </div>
        <ProviderList />
        <div className="relative" aria-hidden="true">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t border-crm-border" />
          </div>
          <div className="relative flex justify-center">
            <span className="bg-crm-background px-2 text-xs uppercase tracking-wide text-crm-text-secondary">
              ou
            </span>
          </div>
        </div>
        <LoginForm />
        <p className="text-center text-sm text-crm-text-secondary">
          <Link
            href="/register"
            className="font-medium text-crm-primary underline-offset-4 hover:underline"
          >
            Criar conta
          </Link>
          <span className="mx-2 text-crm-border" aria-hidden="true">
            ·
          </span>
          <Link
            href="/forgot-password"
            className="font-medium text-crm-primary underline-offset-4 hover:underline"
          >
            Esqueci minha senha
          </Link>
        </p>
      </div>
    </Suspense>
  );
}
