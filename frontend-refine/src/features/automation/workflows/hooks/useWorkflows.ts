import { useQuery, useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
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

function invalidateWorkflowsKeys(companyId: string | null): unknown[][] {
  return [
    ["workflows", companyId],
    ["workflow-executions", companyId],
  ];
}

export function useCreateWorkflow(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateWorkflowRequest) => WorkflowService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Workflow criado",
      invalidateKeys: invalidateWorkflowsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao criar workflow" }),
  });
}

export function useUpdateWorkflow(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateWorkflowRequest }) =>
      WorkflowService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Workflow atualizado",
      invalidateKeys: invalidateWorkflowsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar workflow" }),
  });
}

export function useToggleWorkflow(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, { id: string; active: boolean }>();
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      active
        ? WorkflowService.deactivate(companyId as string, id)
        : WorkflowService.activate(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: (_data, variables) =>
        variables.active ? "Workflow desativado" : "Workflow ativado",
      invalidateKeys: invalidateWorkflowsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao alterar status do workflow" }),
  });
}

export function useDeleteWorkflow(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => WorkflowService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Workflow excluído",
      invalidateKeys: invalidateWorkflowsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao excluir workflow" }),
  });
}
