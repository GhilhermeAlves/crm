import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ActivityService } from "../services/activity.service";
import type { CreateActivityRequest, UpdateActivityRequest } from "../types/activity.types";

export function useActivities(companyId: string | null, filter: { contactId?: string; opportunityId?: string } = {}) {
  return useQuery({
    queryKey: ["activities", companyId, filter.contactId ?? filter.opportunityId ?? "all"],
    queryFn: () => {
      const company = companyId as string;
      if (filter.contactId) {
        return ActivityService.listByContact(company, filter.contactId);
      }
      if (filter.opportunityId) {
        return ActivityService.listByOpportunity(company, filter.opportunityId);
      }
      return ActivityService.list(company);
    },
    enabled: !!companyId,
  });
}

export function useRecentActivities(companyId: string | null, limit = 8) {
  return useQuery({
    queryKey: ["recent-activities", companyId],
    queryFn: () => ActivityService.recent(companyId as string, limit),
    enabled: !!companyId,
  });
}

function useActivityMutations(companyId: string | null) {
  const queryClient = useQueryClient();
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["activities", companyId] });
    queryClient.invalidateQueries({ queryKey: ["recent-activities", companyId] });
    queryClient.invalidateQueries({ queryKey: ["operational-dashboard", companyId] });
  };
  return { queryClient, invalidate };
}

export function useCreateActivity(companyId: string | null) {
  const { invalidate } = useActivityMutations(companyId);
  return useMutation({
    mutationFn: (data: CreateActivityRequest) =>
      ActivityService.create(companyId as string, data),
    onSuccess: () => {
      invalidate();
      toast.success("Atividade registrada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao registrar atividade");
    },
  });
}

export function useUpdateActivity(companyId: string | null) {
  const { invalidate } = useActivityMutations(companyId);
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateActivityRequest }) =>
      ActivityService.update(companyId as string, id, data),
    onSuccess: () => {
      invalidate();
      toast.success("Atividade atualizada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar atividade");
    },
  });
}

export function useDeleteActivity(companyId: string | null) {
  const { invalidate } = useActivityMutations(companyId);
  return useMutation({
    mutationFn: (id: string) => ActivityService.delete(companyId as string, id),
    onSuccess: () => {
      invalidate();
      toast.success("Atividade excluída");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir atividade");
    },
  });
}