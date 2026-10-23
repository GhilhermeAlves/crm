import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { procedureService } from "../services/procedure.service";
import type {
  CompleteProcedureRequest,
  PatientProcedureRequest,
  PatientProcedureResponse,
} from "../types/procedure.types";

export const procedureKeys = {
  all: (companyId: string, contactId: string) => ["patient-procedures", companyId, contactId],
};

export function useProcedures(companyId: string, contactId: string) {
  return useQuery({
    queryKey: procedureKeys.all(companyId, contactId),
    queryFn: () => procedureService.list(companyId, contactId, {}),
    enabled: Boolean(companyId && contactId),
  });
}

// A chave raiz real do financeiro é ["finance", companyId] (ver useFinance.ts).
const invalidate = (companyId: string, contactId: string) => [
  procedureKeys.all(companyId, contactId),
  ["finance", companyId],
];

export function useCreateProcedure(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<
    PatientProcedureResponse,
    PatientProcedureRequest
  >();
  return useMutation({
    mutationFn: (payload: PatientProcedureRequest) =>
      procedureService.create(companyId, contactId, payload),
    onSuccess: onSuccess({
      successMessage: "Procedimento salvo",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível salvar o procedimento" }),
  });
}

type UpdateVars = { procedureId: string; payload: PatientProcedureRequest };

export function useUpdateProcedure(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<PatientProcedureResponse, UpdateVars>();
  return useMutation({
    mutationFn: ({ procedureId, payload }: UpdateVars) =>
      procedureService.update(companyId, contactId, procedureId, payload),
    onSuccess: onSuccess({
      successMessage: "Procedimento salvo",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível salvar o procedimento" }),
  });
}

type CompleteVars = { procedureId: string; payload: CompleteProcedureRequest };

export function useCompleteProcedure(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<PatientProcedureResponse, CompleteVars>();
  return useMutation({
    mutationFn: ({ procedureId, payload }: CompleteVars) =>
      procedureService.complete(companyId, contactId, procedureId, payload),
    onSuccess: onSuccess({
      successMessage: "Procedimento marcado como realizado",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível concluir o procedimento" }),
  });
}

export function useDeleteProcedure(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<void, string>();
  return useMutation({
    mutationFn: (procedureId: string) => procedureService.delete(companyId, contactId, procedureId),
    onSuccess: onSuccess({
      successMessage: "Procedimento excluído",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível excluir o procedimento" }),
  });
}
