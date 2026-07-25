"use client";

import { Suspense } from "react";
import { LoginForm } from "@/features/auth/components/LoginForm";

export default function LoginPage() {
  return (
    <Suspense>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-2xl font-bold">Entrar</h1>
          <p className="text-sm text-muted-foreground">
            Acesse sua conta para continuar
          </p>
          <p className="text-xs text-muted-foreground/60">v1.0.1</p>
        </div>
        <LoginForm />
      </div>
    </Suspense>
  );
}
