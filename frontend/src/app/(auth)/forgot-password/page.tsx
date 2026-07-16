"use client";

import { ForgotPasswordForm } from "@/features/auth/components/ForgotPasswordForm";

export default function ForgotPasswordPage() {
  return (
    <div className="space-y-6">
      <div className="space-y-2 text-center">
        <h1 className="text-2xl font-bold">Esqueceu a senha?</h1>
      </div>
      <ForgotPasswordForm />
    </div>
  );
}
