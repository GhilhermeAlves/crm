import api from "@/lib/api";
import type { Chair, CreateChairRequest, UpdateChairRequest } from "../types/chair.types";

export class ChairService {
  static async list(companyId: string): Promise<Chair[]> {
    const response = await api.get<Chair[]>(`/api/v1/companies/${companyId}/chairs`);
    return response.data;
  }

  static async getById(companyId: string, id: string): Promise<Chair> {
    const response = await api.get<Chair>(`/api/v1/companies/${companyId}/chairs/${id}`);
    return response.data;
  }

  static async create(companyId: string, data: CreateChairRequest): Promise<Chair> {
    const response = await api.post<Chair>(`/api/v1/companies/${companyId}/chairs`, data);
    return response.data;
  }

  static async update(companyId: string, id: string, data: UpdateChairRequest): Promise<Chair> {
    const response = await api.put<Chair>(`/api/v1/companies/${companyId}/chairs/${id}`, data);
    return response.data;
  }

  static async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`/api/v1/companies/${companyId}/chairs/${id}`);
  }
}
