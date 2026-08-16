import api from "@/lib/api";
import type {
  Activity,
  CreateActivityRequest,
  UpdateActivityRequest,
} from "../types/activity.types";

const BASE = "/companies";

export const ActivityService = {
  async list(companyId: string): Promise<Activity[]> {
    const response = await api.get<Activity[]>(
      `${BASE}/${companyId}/activities`,
    );
    return response.data;
  },

  async recent(companyId: string, limit = 10): Promise<Activity[]> {
    const response = await api.get<Activity[]>(
      `${BASE}/${companyId}/activities/recent?limit=${limit}`,
    );
    return response.data;
  },

  async listByContact(
    companyId: string,
    contactId: string,
  ): Promise<Activity[]> {
    const response = await api.get<Activity[]>(
      `${BASE}/${companyId}/contacts/${contactId}/activities`,
    );
    return response.data;
  },

  async listByOpportunity(
    companyId: string,
    opportunityId: string,
  ): Promise<Activity[]> {
    const response = await api.get<Activity[]>(
      `${BASE}/${companyId}/opportunities/${opportunityId}/activities`,
    );
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Activity> {
    const response = await api.get<Activity>(
      `${BASE}/${companyId}/activities/${id}`,
    );
    return response.data;
  },

  async create(
    companyId: string,
    data: CreateActivityRequest,
  ): Promise<Activity> {
    const response = await api.post<Activity>(
      `${BASE}/${companyId}/activities`,
      data,
    );
    return response.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateActivityRequest,
  ): Promise<Activity> {
    const response = await api.put<Activity>(
      `${BASE}/${companyId}/activities/${id}`,
      data,
    );
    return response.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/activities/${id}`);
  },
};
