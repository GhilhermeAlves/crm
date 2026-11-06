"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Dados também mudam fora desta aba (Leo, Telegram, outros usuários):
            // revalida ao abrir a tela ou voltar para a aba, sem precisar de F5.
            staleTime: 30 * 1000,
            retry: 2,
            refetchOnWindowFocus: true,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
