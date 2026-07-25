"use client";

import { Suspense } from "react";
import { AuthCallbackContent } from "./content";

export default function AuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen flex-col items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="mt-4 text-sm text-muted-foreground">Processando autenticação...</p>
        </div>
      }
    >
      <AuthCallbackContent />
    </Suspense>
  );
}
