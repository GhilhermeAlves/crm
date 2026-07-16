"use client";

import { Suspense } from "react";
import { ResetPasswordForm } from "@/features/auth/components/ResetPasswordForm";

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-2xl font-bold">Redefinir senha</h1>
        </div>
        <ResetPasswordForm />
      </div>
    </Suspense>
  );
}
