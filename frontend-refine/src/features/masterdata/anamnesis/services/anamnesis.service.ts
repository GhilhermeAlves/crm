import api from "@/lib/api";
import type {
  AnamnesisModel,
  AnamnesisModelSummary,
  CreateAnamnesisModelRequest,
  UpdateAnamnesisModelRequest,
} from "../types/anamnesis.types";

const base = (companyId: string) => `/companies/${companyId}/anamnesis-models`;

export const AnamnesisService = {
  async list(companyId: string): Promise<AnamnesisModelSummary[]> {
    const response = await api.get<AnamnesisModelSummary[]>(base(companyId));
    return response.data;
  },

  async get(companyId: string, modelId: string): Promise<AnamnesisModel> {
    const response = await api.get<AnamnesisModel>(`${base(companyId)}/${modelId}`);
    return response.data;
  },

  async create(companyId: string, data: CreateAnamnesisModelRequest): Promise<AnamnesisModel> {
    const response = await api.post<AnamnesisModel>(base(companyId), data);
    return response.data;
  },

  async update(
    companyId: string,
    modelId: string,
    data: UpdateAnamnesisModelRequest,
  ): Promise<AnamnesisModel> {
    const response = await api.put<AnamnesisModel>(`${base(companyId)}/${modelId}`, data);
    return response.data;
  },

  async setActive(companyId: string, modelId: string, active: boolean): Promise<AnamnesisModel> {
    const response = await api.post<AnamnesisModel>(
      `${base(companyId)}/${modelId}/${active ? "activate" : "deactivate"}`,
    );
    return response.data;
  },

  async remove(companyId: string, modelId: string): Promise<void> {
    await api.delete(`${base(companyId)}/${modelId}`);
  },
};
