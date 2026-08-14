import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { WorkflowService } from "../services/workflow.service";
import type { CreateWorkflowRequest, UpdateWorkflowRequest } from "../types/workflow.types";

export function useWorkflows(companyId: string | null) {
  return useQuery({
    queryKey: ["workflows", companyId],
    queryFn: () => WorkflowService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useWorkflow(companyId: string | null, workflowId: string | null) {
  return useQuery({
    queryKey: ["workflows", companyId, workflowId],
    queryFn: () => WorkflowService.findById(companyId as string, workflowId as string),
    enabled: !!companyId && !!workflowId,
  });
}

export function useWorkflowExecutions(companyId: string | null, workflowId: string | null) {
  return useQuery({
    queryKey: ["workflow-executions", companyId, workflowId],
    queryFn: () => WorkflowService.executions(companyId as string, workflowId as string),
    enabled: !!companyId && !!workflowId,
  });
}

function invalidateWorkflows(queryClient: ReturnType<typeof useQueryClient>, companyId: string | null) {
  queryClient.invalidateQueries({ queryKey: ["workflows", companyId] });
  queryClient.invalidateQueries({ queryKey: ["workflow-executions", companyId] });
}

export function useCreateWorkflow(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateWorkflowRequest) => WorkflowService.create(companyId as string, data),
    onSuccess: () => {
      invalidateWorkflows(queryClient, companyId);
      toast.success("Workflow criado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar workflow");
    },
  });
}

export function useUpdateWorkflow(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateWorkflowRequest }) =>
      WorkflowService.update(companyId as string, id, data),
    onSuccess: () => {
      invalidateWorkflows(queryClient, companyId);
      toast.success("Workflow atualizado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar workflow");
    },
  });
}

export function useToggleWorkflow(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      active
        ? WorkflowService.deactivate(companyId as string, id)
        : WorkflowService.activate(companyId as string, id),
    onSuccess: (_data, variables) => {
      invalidateWorkflows(queryClient, companyId);
      toast.success(variables.active ? "Workflow desativado" : "Workflow ativado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao alterar status do workflow");
    },
  });
}

export function useDeleteWorkflow(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => WorkflowService.delete(companyId as string, id),
    onSuccess: () => {
      invalidateWorkflows(queryClient, companyId);
      toast.success("Workflow excluído");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir workflow");
    },
  });
}
