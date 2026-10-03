export type TaskStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH";

export type Task = {
  id: string;
  companyId: string;
  contactId: string | null;
  opportunityId: string | null;
  title: string;
  description: string | null;
  assigneeId: string | null;
  dueAt: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  completedAt: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateTaskRequest = {
  contactId?: string;
  opportunityId?: string;
  title: string;
  description?: string;
  assigneeId?: string;
  dueAt?: string;
  priority?: TaskPriority;
};

export type UpdateTaskRequest = {
  title?: string;
  description?: string;
  assigneeId?: string;
  dueAt?: string;
  priority?: TaskPriority;
  contactId?: string;
  opportunityId?: string;
};

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "Pendente",
  IN_PROGRESS: "Em andamento",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: "Baixa",
  MEDIUM: "Média",
  HIGH: "Alta",
};
