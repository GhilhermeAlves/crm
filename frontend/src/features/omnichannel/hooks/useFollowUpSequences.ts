import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { FollowUpSequenceService } from "../services/followup-sequence.service";
import type { FollowUpSequenceRequest } from "../types/followup-sequence.types";

export function useFollowUpSequences(page = 0, pageSize = 20) {
  return useQuery({
    queryKey: ["omnichannel", "follow-up-sequences", page, pageSize],
    queryFn: () => FollowUpSequenceService.list(page, pageSize),
  });
}

export function useFollowUpSequence(sequenceId: string | null) {
  return useQuery({
    queryKey: ["omnichannel", "follow-up-sequence", sequenceId],
    queryFn: () => FollowUpSequenceService.get(sequenceId as string),
    enabled: !!sequenceId,
  });
}

export function useCreateFollowUpSequence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: FollowUpSequenceRequest) =>
      FollowUpSequenceService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "follow-up-sequences"],
      });
      toast.success("Sequência de follow-up criada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar sequência");
    },
  });
}

export function useUpdateFollowUpSequence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: FollowUpSequenceRequest }) =>
      FollowUpSequenceService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "follow-up-sequences"],
      });
      toast.success("Sequência atualizada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar sequência");
    },
  });
}

export function useDeleteFollowUpSequence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => FollowUpSequenceService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "follow-up-sequences"],
      });
      toast.success("Sequência excluída");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir sequência");
    },
  });
}

export function useActivateFollowUpSequence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => FollowUpSequenceService.activate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "follow-up-sequences"],
      });
      toast.success("Sequência ativada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao ativar sequência");
    },
  });
}

export function useDeactivateFollowUpSequence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => FollowUpSequenceService.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "follow-up-sequences"],
      });
      toast.success("Sequência desativada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao desativar sequência");
    },
  });
}
