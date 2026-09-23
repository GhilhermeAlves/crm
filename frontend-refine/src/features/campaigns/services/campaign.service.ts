import api from "@/lib/api";
import type {
  AttachChannelRequest,
  Campaign,
  CampaignExecution,
  CreateCampaignRequest,
  ListCampaignsParams,
  MessageTemplate,
  PageResponse,
  ScheduleCampaignRequest,
  UpdateCampaignRequest,
} from "../types/campaign.types";

const BASE = "/companies";

export const CampaignService = {
  async list(companyId: string, params?: ListCampaignsParams): Promise<PageResponse<Campaign>> {
    const response = await api.get<PageResponse<Campaign>>(`${BASE}/${companyId}/campaigns`, {
      params,
    });
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Campaign> {
    const response = await api.get<Campaign>(`${BASE}/${companyId}/campaigns/${id}`);
    return response.data;
  },

  async create(companyId: string, data: CreateCampaignRequest): Promise<Campaign> {
    const response = await api.post<Campaign>(`${BASE}/${companyId}/campaigns`, data);
    return response.data;
  },

  async update(companyId: string, id: string, data: UpdateCampaignRequest): Promise<Campaign> {
    const response = await api.put<Campaign>(`${BASE}/${companyId}/campaigns/${id}`, data);
    return response.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/campaigns/${id}`);
  },

  async attachChannel(
    companyId: string,
    id: string,
    data: AttachChannelRequest,
  ): Promise<Campaign> {
    const response = await api.post<Campaign>(`${BASE}/${companyId}/campaigns/${id}/channel`, data);
    return response.data;
  },

  async schedule(companyId: string, id: string, data: ScheduleCampaignRequest): Promise<Campaign> {
    const response = await api.post<Campaign>(
      `${BASE}/${companyId}/campaigns/${id}/schedule`,
      data,
    );
    return response.data;
  },

  async executeNow(companyId: string, id: string): Promise<CampaignExecution> {
    const response = await api.post<CampaignExecution>(
      `${BASE}/${companyId}/campaigns/${id}/execute`,
    );
    return response.data;
  },

  async pause(companyId: string, id: string): Promise<Campaign> {
    const response = await api.post<Campaign>(`${BASE}/${companyId}/campaigns/${id}/pause`);
    return response.data;
  },

  async resume(companyId: string, id: string): Promise<Campaign> {
    const response = await api.post<Campaign>(`${BASE}/${companyId}/campaigns/${id}/resume`);
    return response.data;
  },

  async cancel(companyId: string, id: string): Promise<Campaign> {
    const response = await api.post<Campaign>(`${BASE}/${companyId}/campaigns/${id}/cancel`);
    return response.data;
  },

  async getExecution(companyId: string, id: string): Promise<CampaignExecution> {
    const response = await api.get<CampaignExecution>(
      `${BASE}/${companyId}/campaigns/${id}/execution`,
    );
    return response.data;
  },
};

export type ListTemplatesParams = {
  page?: number;
  pageSize?: number;
  channelType?: string;
  status?: "ACTIVE" | "ARCHIVED";
};

type CreateTemplateRequest = {
  name: string;
  channelType?: string;
  subject?: string;
  body: string;
};

export const TemplateService = {
  async list(
    companyId: string,
    params?: ListTemplatesParams,
  ): Promise<PageResponse<MessageTemplate>> {
    const response = await api.get<PageResponse<MessageTemplate>>(
      `${BASE}/${companyId}/templates`,
      { params },
    );
    return response.data;
  },

  async create(companyId: string, data: CreateTemplateRequest): Promise<MessageTemplate> {
    const response = await api.post<MessageTemplate>(`${BASE}/${companyId}/templates`, data);
    return response.data;
  },
};
