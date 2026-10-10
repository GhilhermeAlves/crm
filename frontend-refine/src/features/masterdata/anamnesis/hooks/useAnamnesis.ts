import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { AnamnesisService } from "../services/anamnesis.service";
import type {
  AnamnesisModel,
  CreateAnamnesisModelRequest,
  UpdateAnamnesisModelRequest,
} from "../types/anamnesis.types";

const LIST_KEY = (companyId: string | null) => ["anamnesis-models", companyId];
const DETAIL_KEY = (companyId: string | null, modelId: string | null) => [
  "anamnesis-model",
  companyId,
  modelId,
];

export function useAnamnesisModels(companyId: string | null) {
  return useQuery({
    queryKey: LIST_KEY(companyId),
    queryFn: () => AnamnesisService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useAnamnesisModel(companyId: string | null, modelId: string | null) {
  return useQuery({
    queryKey: DETAIL_KEY(companyId, modelId),
    queryFn: () => AnamnesisService.get(companyId as string, modelId as string),
    enabled: !!companyId && !!modelId,
  });
}

export function useCreateAnamnesisModel(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateAnamnesisModelRequest) =>
      AnamnesisService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Modelo criado",
      invalidateKeys: [LIST_KEY(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao criar modelo" }),
  });
}

export function useUpdateAnamnesisModel(companyId: string | null, modelId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: UpdateAnamnesisModelRequest) =>
      AnamnesisService.update(companyId as string, modelId as string, data),
    onSuccess: onSuccess({
      successMessage: "Modelo salvo",
      invalidateKeys: [LIST_KEY(companyId), DETAIL_KEY(companyId, modelId)],
    }),
    onError: onError({ errorMessage: "Erro ao salvar modelo" }),
  });
}

export function useSetAnamnesisModelActive(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<
    AnamnesisModel,
    { modelId: string; active: boolean }
  >();
  return useMutation({
    mutationFn: ({ modelId, active }: { modelId: string; active: boolean }) =>
      AnamnesisService.setActive(companyId as string, modelId, active),
    onSuccess: onSuccess({
      successMessage: (_data, vars) => (vars.active ? "Modelo ativado" : "Modelo desativado"),
      invalidateKeys: [LIST_KEY(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao alterar o status do modelo" }),
  });
}

export function useDeleteAnamnesisModel(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (modelId: string) => AnamnesisService.remove(companyId as string, modelId),
    onSuccess: onSuccess({
      successMessage: "Modelo excluído",
      invalidateKeys: [LIST_KEY(companyId)],
    }),
    onError: onError({ errorMessage: "Erro ao excluir modelo" }),
  });
}
