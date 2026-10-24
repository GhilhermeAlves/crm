"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { TrichoscopyService } from "../services/trichoscopy.service";

export const trichoscopyKeys = {
  all: (companyId: string, contactId: string) => ["trichoscopy", companyId, contactId],
};

export function useTrichoscopyList(companyId: string, contactId: string) {
  return useQuery({
    queryKey: trichoscopyKeys.all(companyId, contactId),
    queryFn: () => TrichoscopyService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function useCaptureTrichoscopy(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ file, notes }: { file: Blob; notes?: string }) =>
      TrichoscopyService.capture(companyId, contactId, file, notes),
    onSuccess: onSuccess({
      successMessage: "Imagem salva",
      invalidateKeys: [trichoscopyKeys.all(companyId, contactId)],
    }),
    onError: onError({ errorMessage: "Erro ao salvar a imagem" }),
  });
}

export function useDeleteTrichoscopy(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (imageId: string) => TrichoscopyService.remove(companyId, contactId, imageId),
    onSuccess: onSuccess({
      successMessage: "Imagem excluída",
      invalidateKeys: [trichoscopyKeys.all(companyId, contactId)],
    }),
    onError: onError({ errorMessage: "Erro ao excluir a imagem" }),
  });
}
