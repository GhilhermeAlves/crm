"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { CompanyModulesService } from "../services/modules.service";
import type { CompanyModuleName, UpdateCompanyModulesRequest } from "../types/modules.types";

const QUERY_KEY = "company-modules";

export function useCompanyModules(companyId: string | null | undefined) {
  return useQuery({
    queryKey: [QUERY_KEY, companyId],
    queryFn: () => CompanyModulesService.get(companyId as string),
    enabled: !!companyId,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}

/**
 * Módulos da empresa ativa. Enquanto carrega (ou se a consulta falhar) tudo
 * conta como habilitado — mesmo critério do menu com permissões vazias —
 * para não esconder telas por um erro transitório; o backend continua
 * barrando com 403 MODULE_DISABLED.
 */
export function useActiveCompanyModules() {
  const { user } = useAuth();
  const query = useCompanyModules(user?.companyId ?? null);
  const enabledModules = query.data?.enabledModules ?? null;

  const isEnabled = (module: CompanyModuleName) =>
    enabledModules === null || enabledModules.includes(module);

  return { enabledModules, isEnabled, isLoading: query.isLoading };
}

export function useUpdateCompanyModules(companyId: string) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: UpdateCompanyModulesRequest) =>
      CompanyModulesService.update(companyId, data),
    onSuccess: onSuccess({
      successMessage: "Módulos da empresa atualizados",
      invalidateKeys: [[QUERY_KEY]],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar os módulos" }),
  });
}
