"use client";

import { useQuery } from "@tanstack/react-query";
import { UsageService } from "../services/usage.service";

/**
 * Uso/quota da empresa ativa (Sprint 8.6): usuários, contatos e armazenamento
 * com os limites do plano. Desabilitado quando não há empresa ativa.
 */
export function useCompanyUsage(companyId?: string | null, enabled = true) {
  return useQuery({
    queryKey: ["usage", companyId],
    queryFn: () => UsageService.companyUsage(companyId as string),
    enabled: Boolean(companyId) && enabled,
    staleTime: 60 * 1000,
    retry: false,
  });
}
