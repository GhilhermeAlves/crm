"use client";

import { useEffect } from "react";
import { ErrorPage } from "@/components/feedback/ErrorPage";

export default function Error({
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
    <div className="flex min-h-screen items-center justify-center">
      <ErrorPage
        code="500"
        title="Erro interno"
        description="Algo deu errado no servidor. Tente novamente mais tarde."
      />
    </div>
  );
}
