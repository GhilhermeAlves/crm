"use client";

import { useQuery } from "@tanstack/react-query";
import type { IdentityProviderInfo } from "../types/identity-provider";

/**
 * Catálogo público de provedores de identidade (GET /auth/providers — Sprint
 * 7.0). A disponibilidade é decidida no servidor; o browser apenas exibe o que
 * o catálogo devolve (nenhum segredo, nenhuma escolha de bucket/limite).
 */
export function useIdentityProviders() {
  return useQuery({
    queryKey: ["identity-providers"],
    queryFn: async (): Promise<IdentityProviderInfo[]> => {
      const response = await fetch("/auth/providers", { credentials: "include" });
      if (!response.ok) {
        throw new Error("Falha ao carregar os provedores de identidade.");
      }
      return (await response.json()) as IdentityProviderInfo[];
    },
    retry: false,
    staleTime: 5 * 60 * 1000,
  });
}
