import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { RecallService, type RecallListParams } from "../services/recall.service";
import type { CreateRecallRequest } from "../types/recall.types";

function invalidateRecallKeys(companyId: string | null): unknown[][] {
  return [
    ["recalls", companyId],
    ["recall-suggestions", companyId],
  ];
}

export function useRecalls(companyId: string | null, params: RecallListParams) {
  return useQuery({
    queryKey: ["recalls", companyId, params],
    queryFn: () => RecallService.list(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useRecallSuggestions(companyId: string | null, since: number) {
  return useQuery({
    queryKey: ["recall-suggestions", companyId, since],
    queryFn: () => RecallService.suggestions(companyId as string, since),
    enabled: !!companyId,
  });
}

export function useCreateRecall(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateRecallRequest) => RecallService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Retorno criado",
      invalidateKeys: [["recalls", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao criar retorno" }),
  });
}

export function useDismissRecall(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (recallId: string) => RecallService.dismiss(companyId as string, recallId),
    onSuccess: onSuccess({
      successMessage: "Retorno descartado",
      invalidateKeys: [["recalls", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao descartar retorno" }),
  });
}

export function useScheduleRecall(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ recallId, appointmentId }: { recallId: string; appointmentId: string }) =>
      RecallService.schedule(companyId as string, recallId, appointmentId),
    onSuccess: onSuccess({
      successMessage: "Retorno vinculado ao agendamento",
      invalidateKeys: invalidateRecallKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao vincular retorno ao agendamento" }),
  });
}

export function useDismissRecallSuggestion(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (contactId: string) =>
      RecallService.dismissSuggestion(companyId as string, contactId),
    onSuccess: onSuccess({
      successMessage: "Sugestão descartada",
      invalidateKeys: [["recall-suggestions", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao descartar sugestão" }),
  });
}
