import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { TaskService } from "../services/task.service";
import type {
  CreateTaskRequest,
  TaskStatus,
  UpdateTaskRequest,
} from "../types/task.types";

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

export function useTasksByOpportunity(
  companyId: string | null,
  opportunityId: string | null,
) {
  return useQuery({
    queryKey: ["tasks", companyId, "opportunity", opportunityId],
    queryFn: () =>
      TaskService.listByOpportunity(
        companyId as string,
        opportunityId as string,
      ),
    enabled: !!companyId && !!opportunityId,
  });
}

function invalidateTasks(
  queryClient: ReturnType<typeof useQueryClient>,
  companyId: string | null,
) {
  queryClient.invalidateQueries({ queryKey: ["tasks", companyId] });
  queryClient.invalidateQueries({ queryKey: ["tasks-due-today", companyId] });
  queryClient.invalidateQueries({
    queryKey: ["operational-dashboard", companyId],
  });
}

export function useCreateTask(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTaskRequest) =>
      TaskService.create(companyId as string, data),
    onSuccess: () => {
      invalidateTasks(queryClient, companyId);
      toast.success("Tarefa criada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar tarefa");
    },
  });
}

export function useUpdateTask(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTaskRequest }) =>
      TaskService.update(companyId as string, id, data),
    onSuccess: () => {
      invalidateTasks(queryClient, companyId);
      toast.success("Tarefa atualizada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar tarefa");
    },
  });
}

export function useChangeTaskStatus(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: TaskStatus }) =>
      TaskService.changeStatus(companyId as string, id, status),
    onSuccess: () => {
      invalidateTasks(queryClient, companyId);
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar status");
    },
  });
}

export function useDeleteTask(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TaskService.delete(companyId as string, id),
    onSuccess: () => {
      invalidateTasks(queryClient, companyId);
      toast.success("Tarefa excluída");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir tarefa");
    },
  });
}
