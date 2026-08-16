"use client";

import { RegisterForm } from "@/features/auth/components/RegisterForm";

export default function RegisterPage() {
  return (
    <div className="space-y-6">
      <div className="space-y-2 text-center">
        <h1 className="text-2xl font-bold">Criar conta</h1>
        <p className="text-sm text-muted-foreground">Preencha os dados abaixo para se cadastrar</p>
      </div>
      <RegisterForm />
    </div>
  );
}
