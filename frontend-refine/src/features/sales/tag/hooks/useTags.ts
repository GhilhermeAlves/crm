import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { TagService } from "../services/tag.service";
import type { CreateTagRequest, UpdateTagRequest } from "../types/tag.types";

export function useTags(companyId: string | null) {
  return useQuery({
    queryKey: ["tags", companyId],
    queryFn: () => TagService.list(companyId!),
    enabled: !!companyId,
  });
}

export function useCreateTag(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTagRequest) => TagService.create(companyId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tags", companyId] });
    },
  });
}

export function useUpdateTag(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTagRequest }) =>
      TagService.update(companyId!, id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tags", companyId] });
    },
  });
}

export function useDeleteTag(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TagService.delete(companyId!, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tags", companyId] });
    },
  });
}
