import api from "@/lib/api";
import type {
  Lead,
  CreateLeadRequest,
  UpdateLeadRequest,
  ListLeadsParams,
  PageResponse,
} from "../types/lead.types";

const BASE = "/companies";

export const LeadService = {
  async list(
    companyId: string,
    params?: ListLeadsParams,
  ): Promise<PageResponse<Lead>> {
    const response = await api.get<PageResponse<Lead>>(
      `${BASE}/${companyId}/leads`,
      { params },
    );
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Lead> {
    const response = await api.get<Lead>(`${BASE}/${companyId}/leads/${id}`);
    return response.data;
  },

  async create(companyId: string, data: CreateLeadRequest): Promise<Lead> {
    const response = await api.post<Lead>(`${BASE}/${companyId}/leads`, data);
    return response.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateLeadRequest,
  ): Promise<Lead> {
    const response = await api.put<Lead>(
      `${BASE}/${companyId}/leads/${id}`,
      data,
    );
    return response.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/leads/${id}`);
  },
};
