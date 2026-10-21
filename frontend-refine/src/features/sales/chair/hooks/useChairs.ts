import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChairService } from "../services/chair.service";
import type { CreateChairRequest, UpdateChairRequest } from "../types/chair.types";

export function useChairs(companyId: string | null) {
  return useQuery({
    queryKey: ["chairs", companyId],
    queryFn: () => ChairService.list(companyId!),
    enabled: !!companyId,
  });
}

export function useCreateChair(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateChairRequest) => ChairService.create(companyId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chairs", companyId] });
    },
  });
}

export function useUpdateChair(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateChairRequest }) =>
      ChairService.update(companyId!, id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chairs", companyId] });
    },
  });
}

export function useDeleteChair(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => ChairService.delete(companyId!, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chairs", companyId] });
    },
  });
}
