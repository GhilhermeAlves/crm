import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PipelineService } from "../services/pipeline.service";
import type { CreatePipelineRequest } from "../types/pipeline.types";

export function usePipelines(companyId: string | null) {
  return useQuery({
    queryKey: ["pipelines", companyId],
    queryFn: () => PipelineService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function usePipeline(companyId: string | null, id: string | null) {
  return useQuery({
    queryKey: ["pipelines", companyId, id],
    queryFn: () => PipelineService.findById(companyId as string, id as string),
    enabled: !!companyId && !!id,
  });
}

export function useCreatePipeline(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePipelineRequest) => PipelineService.create(companyId as string, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pipelines", companyId] });
      toast.success("Pipeline criado com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar pipeline");
    },
  });
}
