import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
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
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateLeadRequest) => LeadService.create(companyId as string, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["leads", companyId] });
      toast.success("Lead criado com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar lead");
    },
  });
}

export function useUpdateLead(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLeadRequest }) =>
      LeadService.update(companyId as string, id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["leads", companyId] });
      queryClient.invalidateQueries({ queryKey: ["leads", companyId, id] });
      toast.success("Lead atualizado com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar lead");
    },
  });
}

export function useDeleteLead(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => LeadService.delete(companyId as string, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["leads", companyId] });
      toast.success("Lead excluído com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir lead");
    },
  });
}
