import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { CatalogService } from "../services/catalog.service";
import type { CatalogFilters, CatalogItem, CatalogItemRequest } from "../types/catalog.types";

type SetActiveVars = { id: string; active: boolean };

export function useCatalog(companyId: string | null, filters: CatalogFilters) {
  return useQuery({
    queryKey: ["catalog", companyId, filters],
    queryFn: () => CatalogService.list(companyId as string, filters),
    enabled: !!companyId,
    placeholderData: keepPreviousData,
  });
}

export function useCreateCatalogItem(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CatalogItemRequest) => CatalogService.create(companyId as string, data),
    onSuccess: onSuccess({ successMessage: "Item criado", invalidateKeys: [["catalog", companyId]] }),
    onError: onError({ errorMessage: "Erro ao criar item" }),
  });
}

export function useUpdateCatalogItem(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CatalogItemRequest }) =>
      CatalogService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Item atualizado",
      invalidateKeys: [["catalog", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar item" }),
  });
}

export function useSetCatalogItemActive(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<CatalogItem, SetActiveVars>();
  return useMutation({
    mutationFn: ({ id, active }: SetActiveVars) =>
      CatalogService.setActive(companyId as string, id, active),
    onSuccess: onSuccess({
      successMessage: (_data, vars) => (vars.active ? "Item ativado" : "Item desativado"),
      invalidateKeys: [["catalog", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao alterar o status do item" }),
  });
}
