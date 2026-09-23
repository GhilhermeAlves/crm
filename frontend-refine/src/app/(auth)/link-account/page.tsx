"use client";

import { Suspense } from "react";
import { LoginBrand } from "@/components/brand/LoginBrand";
import { LinkAccountForm } from "@/features/auth/components/LinkAccountForm";

/**
 * Vínculo de conta local (Sprint 7.2, Caso B): exibida quando o login Google
 * usa um e-mail que já possui conta local sem `keycloak_sub`. O usuário verifica
 * a senha da conta local antes de vincular — nunca se auto-vincula por e-mail.
 */
export default function LinkAccountPage() {
  return (
    <Suspense>
      <div className="space-y-6">
        <LoginBrand variant="mobile" wordmark="CRM" className="pt-2" />
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-crm-text">
            Vincular sua conta
          </h1>
          <p className="text-sm text-crm-text-secondary">
            Conecte seu login do Google à conta do CRM
          </p>
        </div>
        <LinkAccountForm />
      </div>
    </Suspense>
  );
}
