import api from "@/lib/api";
import type {
  Tenant,
  CreateTenantRequest,
  UpdateTenantRequest,
  ListTenantsParams,
} from "../types/tenant.types";

const BASE_PATH = "/companies";

export const TenantService = {
  async list(params?: ListTenantsParams): Promise<Tenant[]> {
    const response = await api.get<Tenant[]>(BASE_PATH, { params });
    return response.data;
  },

  async findById(id: string): Promise<Tenant> {
    const response = await api.get<Tenant>(`${BASE_PATH}/${id}`);
    return response.data;
  },

  async create(data: CreateTenantRequest): Promise<Tenant> {
    const response = await api.post<Tenant>(BASE_PATH, data);
    return response.data;
  },

  async update(id: string, data: UpdateTenantRequest): Promise<Tenant> {
    const response = await api.put<Tenant>(`${BASE_PATH}/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`${BASE_PATH}/${id}`);
  },
};
