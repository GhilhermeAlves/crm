import { useQuery, useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { LeadService } from "../services/lead.service";
import type { CreateLeadRequest, ListLeadsParams, UpdateLeadRequest } from "../types/lead.types";

export function useLeads(companyId: string | null, params?: ListLeadsParams) {
  return useQuery({
    queryKey: ["leads", companyId, params],
    queryFn: () => LeadService.list(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useLead(companyId: string | null, id: string) {
  return useQuery({
    queryKey: ["leads", companyId, id],
    queryFn: () => LeadService.findById(companyId as string, id),
    enabled: !!companyId && !!id,
  });
}

export function useCreateLead(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateLeadRequest) => LeadService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Lead criado com sucesso",
      invalidateKeys: [["leads", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao criar lead" }),
  });
}

export function useUpdateLead(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<
    unknown,
    { id: string; data: UpdateLeadRequest }
  >();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLeadRequest }) =>
      LeadService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Lead atualizado com sucesso",
      invalidateKeys: [["leads", companyId]],
      extraInvalidate: (qc, _d, { id }) =>
        qc.invalidateQueries({ queryKey: ["leads", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar lead" }),
  });
}

export function useDeleteLead(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => LeadService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Lead excluído com sucesso",
      invalidateKeys: [["leads", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao excluir lead" }),
  });
}
