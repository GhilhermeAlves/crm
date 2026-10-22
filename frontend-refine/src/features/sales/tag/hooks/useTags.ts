import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { TagService } from "../services/tag.service";
import type { CreateTagRequest, UpdateTagRequest } from "../types/tag.types";

const tagsKey = (companyId: string | null) => ["tags", companyId];

export function useTags(companyId: string | null) {
  return useQuery({
    queryKey: tagsKey(companyId),
    queryFn: () => TagService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useCreateTag(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateTagRequest) => TagService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Marcador criado",
      invalidateKeys: [tagsKey(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao criar marcador" }),
  });
}

export function useUpdateTag(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTagRequest }) =>
      TagService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Marcador atualizado",
      invalidateKeys: [tagsKey(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar marcador" }),
  });
}

export function useDeleteTag(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => TagService.delete(companyId as string, id),
    // Excluir o marcador também o tira dos agendamentos (V101: ON DELETE CASCADE).
    onSuccess: onSuccess({
      successMessage: "Marcador excluído",
      invalidateKeys: [tagsKey(companyId), ["appointments"]],
    }),
    onError: onError({ errorMessage: "Erro ao excluir marcador" }),
  });
}
