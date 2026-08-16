import api from "@/lib/api";
import type {
  Task,
  TaskStatus,
  CreateTaskRequest,
  UpdateTaskRequest,
} from "../types/task.types";

const BASE = "/companies";

export const TaskService = {
  async list(companyId: string, status?: TaskStatus): Promise<Task[]> {
    const query = status ? `?status=${status}` : "";
    const response = await api.get<Task[]>(
      `${BASE}/${companyId}/tasks${query}`,
    );
    return response.data;
  },

  async dueToday(companyId: string): Promise<Task[]> {
    const response = await api.get<Task[]>(
      `${BASE}/${companyId}/tasks/due-today`,
    );
    return response.data;
  },

  async listByOpportunity(
    companyId: string,
    opportunityId: string,
  ): Promise<Task[]> {
    const response = await api.get<Task[]>(
      `${BASE}/${companyId}/opportunities/${opportunityId}/tasks`,
    );
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Task> {
    const response = await api.get<Task>(`${BASE}/${companyId}/tasks/${id}`);
    return response.data;
  },

  async create(companyId: string, data: CreateTaskRequest): Promise<Task> {
    const response = await api.post<Task>(`${BASE}/${companyId}/tasks`, data);
    return response.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateTaskRequest,
  ): Promise<Task> {
    const response = await api.put<Task>(
      `${BASE}/${companyId}/tasks/${id}`,
      data,
    );
    return response.data;
  },

  async changeStatus(
    companyId: string,
    id: string,
    status: TaskStatus,
  ): Promise<Task> {
    const response = await api.post<Task>(
      `${BASE}/${companyId}/tasks/${id}/status/${status}`,
    );
    return response.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/tasks/${id}`);
  },
};
