import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { InventoryService } from "../services/inventory.service";
import type { InventoryFilters, RegisterMovementRequest } from "../types/inventory.types";

export function useInventory(companyId: string | null, filters: InventoryFilters) {
  return useQuery({
    queryKey: ["inventory", companyId, filters],
    queryFn: () => InventoryService.list(companyId as string, filters),
    enabled: !!companyId,
    placeholderData: keepPreviousData,
  });
}

export function useItemMovements(companyId: string | null, itemId: string | null) {
  return useQuery({
    queryKey: ["inventory", companyId, itemId, "movements"],
    queryFn: () => InventoryService.listMovements(companyId as string, itemId as string),
    enabled: !!companyId && !!itemId,
    placeholderData: keepPreviousData,
  });
}

export function useRegisterMovement(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, { itemId: string; request: RegisterMovementRequest }>();
  return useMutation({
    mutationFn: ({ itemId, request }: { itemId: string; request: RegisterMovementRequest }) =>
      InventoryService.registerMovement(companyId as string, itemId, request),
    onSuccess: onSuccess({
      successMessage: "Movimento registrado",
      invalidateKeys: [["inventory", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao registrar movimento" }),
  });
}