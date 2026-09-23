import { useQuery, useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { TaskService } from "../services/task.service";
import type { CreateTaskRequest, TaskStatus, UpdateTaskRequest } from "../types/task.types";

export function useTasks(companyId: string | null, status?: TaskStatus) {
  return useQuery({
    queryKey: ["tasks", companyId, status ?? "all"],
    queryFn: () => TaskService.list(companyId as string, status),
    enabled: !!companyId,
  });
}

export function useTasksDueToday(companyId: string | null) {
  return useQuery({
    queryKey: ["tasks-due-today", companyId],
    queryFn: () => TaskService.dueToday(companyId as string),
    enabled: !!companyId,
  });
}

export function useTasksByOpportunity(companyId: string | null, opportunityId: string | null) {
  return useQuery({
    queryKey: ["tasks", companyId, "opportunity", opportunityId],
    queryFn: () => TaskService.listByOpportunity(companyId as string, opportunityId as string),
    enabled: !!companyId && !!opportunityId,
  });
}

function invalidateTasksKeys(companyId: string | null): unknown[][] {
  return [
    ["tasks", companyId],
    ["tasks-due-today", companyId],
    ["operational-dashboard", companyId],
  ];
}

export function useCreateTask(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateTaskRequest) => TaskService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Tarefa criada",
      invalidateKeys: invalidateTasksKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao criar tarefa" }),
  });
}

export function useUpdateTask(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTaskRequest }) =>
      TaskService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Tarefa atualizada",
      invalidateKeys: invalidateTasksKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar tarefa" }),
  });
}

export function useChangeTaskStatus(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: TaskStatus }) =>
      TaskService.changeStatus(companyId as string, id, status),
    onSuccess: onSuccess({
      invalidateKeys: invalidateTasksKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar status" }),
  });
}

export function useDeleteTask(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => TaskService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Tarefa excluída",
      invalidateKeys: invalidateTasksKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao excluir tarefa" }),
  });
}
