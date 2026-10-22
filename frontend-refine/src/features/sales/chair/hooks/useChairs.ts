import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { ChairService } from "../services/chair.service";
import type { CreateChairRequest, UpdateChairRequest } from "../types/chair.types";

const chairsKey = (companyId: string | null) => ["chairs", companyId];

export function useChairs(companyId: string | null) {
  return useQuery({
    queryKey: chairsKey(companyId),
    queryFn: () => ChairService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useCreateChair(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateChairRequest) => ChairService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Cadeira criada",
      invalidateKeys: [chairsKey(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao criar cadeira" }),
  });
}

export function useUpdateChair(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateChairRequest }) =>
      ChairService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Cadeira atualizada",
      invalidateKeys: [chairsKey(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar cadeira" }),
  });
}

export function useDeleteChair(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => ChairService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Cadeira excluída",
      invalidateKeys: [chairsKey(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao excluir cadeira" }),
  });
}
