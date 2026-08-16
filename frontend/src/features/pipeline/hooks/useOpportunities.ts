import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { OpportunityService } from "../services/pipeline.service";
import type { CreateOpportunityRequest, MoveDirection } from "../types/pipeline.types";

export function useOpportunities(companyId: string | null, pipelineId: string | null) {
  return useQuery({
    queryKey: ["opportunities", companyId, pipelineId],
    queryFn: () => OpportunityService.listByPipeline(companyId as string, pipelineId as string),
    enabled: !!companyId && !!pipelineId,
  });
}

function invalidateOpportunities(
  queryClient: ReturnType<typeof useQueryClient>,
  companyId: string | null,
  pipelineId?: string,
) {
  queryClient.invalidateQueries({ queryKey: ["opportunities", companyId] });
  if (pipelineId) {
    queryClient.invalidateQueries({
      queryKey: ["opportunities", companyId, pipelineId],
    });
  }
}

export function useCreateOpportunity(companyId: string | null, pipelineId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateOpportunityRequest) =>
      OpportunityService.create(companyId as string, pipelineId as string, data),
    onSuccess: () => {
      invalidateOpportunities(queryClient, companyId, pipelineId ?? undefined);
      toast.success("Oportunidade criada com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar oportunidade");
    },
  });
}

export function useMoveOpportunity(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, direction }: { id: string; direction: MoveDirection }) =>
      OpportunityService.move(companyId as string, id, direction),
    onSuccess: () => {
      invalidateOpportunities(queryClient, companyId);
      toast.success("Oportunidade movida");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao mover oportunidade");
    },
  });
}

export function useMarkWonOpportunity(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => OpportunityService.markWon(companyId as string, id),
    onSuccess: () => {
      invalidateOpportunities(queryClient, companyId);
      toast.success("Oportunidade marcada como ganha");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao marcar oportunidade");
    },
  });
}

export function useMarkLostOpportunity(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, lossReason }: { id: string; lossReason: string }) =>
      OpportunityService.markLost(companyId as string, id, lossReason),
    onSuccess: () => {
      invalidateOpportunities(queryClient, companyId);
      toast.success("Oportunidade marcada como perdida");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao marcar oportunidade");
    },
  });
}

export function useDeleteOpportunity(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => OpportunityService.delete(companyId as string, id),
    onSuccess: () => {
      invalidateOpportunities(queryClient, companyId);
      toast.success("Oportunidade excluída");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir oportunidade");
    },
  });
}
