"use client";

import { Suspense } from "react";
import Link from "next/link";
import { ProviderList } from "@/features/auth/components/ProviderList";
import { RegisterForm } from "@/features/auth/components/RegisterForm";

export default function RegisterPage() {
  return (
    <Suspense>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-2xl font-bold">Criar conta</h1>
          <p className="text-sm text-muted-foreground">
            Preencha os dados abaixo ou use um provedor
          </p>
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
        <RegisterForm />
        <p className="text-center text-sm text-muted-foreground">
          Já possui uma conta?{" "}
          <Link
            href="/login"
            className="font-medium text-primary hover:underline"
          >
            Entrar
          </Link>
        </p>
      </div>
    </Suspense>
  );
}
