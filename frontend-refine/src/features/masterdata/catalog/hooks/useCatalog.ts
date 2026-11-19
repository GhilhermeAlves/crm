import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { CatalogService } from "../services/catalog.service";
import type { CatalogFilters, CatalogItem, CatalogItemRequest } from "../types/catalog.types";

type SetActiveVars = { id: string; active: boolean };
type LookupFilters = Pick<CatalogFilters, "q" | "type" | "active">;

export function useCatalog(companyId: string | null, filters: CatalogFilters) {
  return useQuery({
    queryKey: ["catalog", companyId, filters],
    queryFn: () => CatalogService.list(companyId as string, filters),
    enabled: !!companyId,
    placeholderData: keepPreviousData,
  });
}

/** Lookup mínimo do catálogo para selects de outros domínios. */
export function useCatalogLookup(companyId: string | null, filters: LookupFilters) {
  return useQuery({
    queryKey: ["catalog", companyId, "lookup", filters],
    queryFn: () => CatalogService.lookup(companyId as string, filters),
    enabled: !!companyId,
  });
}

/** Catálogo inteiro (todas as páginas), para listas de seleção. */
export function useAllCatalog(
  companyId: string | null,
  filters: Omit<CatalogFilters, "page" | "pageSize">,
  enabled = true,
) {
  return useQuery({
    queryKey: ["catalog", companyId, "all", filters],
    queryFn: () => CatalogService.listAll(companyId as string, filters),
    enabled: !!companyId && enabled,
  });
}

export function useCreateCatalogItem(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CatalogItemRequest) => CatalogService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Item criado",
      invalidateKeys: [["catalog", companyId]],
    }),
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

/**
 * Grava em silêncio (sem toast) o preço de um item do catálogo a partir do valor
 * digitado em outra tela (ex.: procedimento do paciente), para vir preenchido na
 * próxima vez.
 */
export function useRememberCatalogPrice(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ item, priceCents }: { item: CatalogItem; priceCents: number }) =>
      CatalogService.update(companyId as string, item.id, {
        type: item.type,
        name: item.name,
        description: item.description ?? undefined,
        category: item.category ?? undefined,
        price: priceCents / 100,
        currency: item.currency,
        sku: item.sku ?? undefined,
        unit: item.unit,
        minStock: item.minStock,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalog", companyId] }),
  });
}
