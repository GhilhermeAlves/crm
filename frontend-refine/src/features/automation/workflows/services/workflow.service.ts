import api from "@/lib/api";
import type {
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
  Workflow,
  WorkflowExecution,
} from "../types/workflow.types";

const BASE = "/companies";

export const WorkflowService = {
  async list(companyId: string): Promise<Workflow[]> {
    const response = await api.get<Workflow[]>(`${BASE}/${companyId}/workflows`);
    return response.data;
  },

  async findById(companyId: string, workflowId: string): Promise<Workflow> {
    const response = await api.get<Workflow>(`${BASE}/${companyId}/workflows/${workflowId}`);
    return response.data;
  },

  async create(companyId: string, data: CreateWorkflowRequest): Promise<Workflow> {
    const response = await api.post<Workflow>(`${BASE}/${companyId}/workflows`, data);
    return response.data;
  },

  async update(
    companyId: string,
    workflowId: string,
    data: UpdateWorkflowRequest,
  ): Promise<Workflow> {
    const response = await api.put<Workflow>(`${BASE}/${companyId}/workflows/${workflowId}`, data);
    return response.data;
  },

  async activate(companyId: string, workflowId: string): Promise<Workflow> {
    const response = await api.post<Workflow>(
      `${BASE}/${companyId}/workflows/${workflowId}/activate`,
    );
    return response.data;
  },

  async deactivate(companyId: string, workflowId: string): Promise<Workflow> {
    const response = await api.post<Workflow>(
      `${BASE}/${companyId}/workflows/${workflowId}/deactivate`,
    );
    return response.data;
  },

  async delete(companyId: string, workflowId: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/workflows/${workflowId}`);
  },

  async executions(companyId: string, workflowId: string): Promise<WorkflowExecution[]> {
    const response = await api.get<WorkflowExecution[]>(
      `${BASE}/${companyId}/workflows/${workflowId}/executions`,
    );
    return response.data;
  },
};
