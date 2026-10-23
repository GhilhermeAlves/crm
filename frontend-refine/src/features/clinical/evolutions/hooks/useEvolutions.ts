import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { evolutionService } from "../services/evolution.service";
import type { PatientEvolutionRequest } from "../types/evolution.types";

export const evolutionKeys = {
  all: (companyId: string, contactId: string) =>
    ["patient-evolutions", companyId, contactId] as const,
};

export function useEvolutions(companyId: string, contactId: string) {
  return useQuery({
    queryKey: evolutionKeys.all(companyId, contactId),
    queryFn: () => evolutionService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function useCreateEvolution(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: PatientEvolutionRequest) =>
      evolutionService.create(companyId, contactId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: evolutionKeys.all(companyId, contactId) });
      toast.success("Evolução salva");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível salvar a evolução");
    },
  });
}

export function useUpdateEvolution(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      evolutionId,
      payload,
    }: {
      evolutionId: string;
      payload: PatientEvolutionRequest;
    }) => evolutionService.update(companyId, contactId, evolutionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: evolutionKeys.all(companyId, contactId) });
      toast.success("Evolução salva");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível salvar a evolução");
    },
  });
}

export function useFinalizeEvolution(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (evolutionId: string) =>
      evolutionService.finalize(companyId, contactId, evolutionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: evolutionKeys.all(companyId, contactId) });
      toast.success("Evolução finalizada");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível finalizar a evolução");
    },
  });
}

export function useDeleteEvolution(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (evolutionId: string) => evolutionService.delete(companyId, contactId, evolutionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: evolutionKeys.all(companyId, contactId) });
      toast.success("Evolução excluída");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível excluir a evolução");
    },
  });
}
