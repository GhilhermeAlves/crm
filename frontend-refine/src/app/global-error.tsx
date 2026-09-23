"use client";

import { useEffect } from "react";
import { ErrorPage } from "@/components/feedback/ErrorPage";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <html lang="pt-BR">
      <body>
        <ErrorPage
          code="500"
          title="Erro inesperado"
          description="Ocorreu um erro inesperado. Por favor, recarregue a página."
        />
      </body>
    </html>
  );
}
