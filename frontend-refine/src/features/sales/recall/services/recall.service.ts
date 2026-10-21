import api from "@/lib/api";
import type {
  CreateRecallRequest,
  Recall,
  RecallPageResponse,
  RecallSuggestion,
  UpdateRecallRequest,
} from "../types/recall.types";

const BASE = "/companies";

export type RecallListParams = {
  hostId?: string;
  tag?: string;
  q?: string;
  window: string;
  page?: number;
  pageSize?: number;
};

export const RecallService = {
  async list(companyId: string, params: RecallListParams): Promise<RecallPageResponse<Recall>> {
    const search = new URLSearchParams({ window: params.window });
    if (params.hostId) search.set("hostId", params.hostId);
    if (params.tag) search.set("tag", params.tag);
    if (params.q) search.set("q", params.q);
    if (params.page !== undefined) search.set("page", String(params.page));
    if (params.pageSize !== undefined) search.set("pageSize", String(params.pageSize));
    const response = await api.get<RecallPageResponse<Recall>>(
      `${BASE}/${companyId}/recalls?${search.toString()}`,
    );
    return response.data;
  },

  async create(companyId: string, data: CreateRecallRequest): Promise<Recall> {
    const response = await api.post<Recall>(`${BASE}/${companyId}/recalls`, data);
    return response.data;
  },

  async update(companyId: string, recallId: string, data: UpdateRecallRequest): Promise<Recall> {
    const response = await api.patch<Recall>(`${BASE}/${companyId}/recalls/${recallId}`, data);
    return response.data;
  },

  async dismiss(companyId: string, recallId: string): Promise<Recall> {
    const response = await api.post<Recall>(`${BASE}/${companyId}/recalls/${recallId}/dismiss`);
    return response.data;
  },

  async schedule(companyId: string, recallId: string, appointmentId: string): Promise<Recall> {
    const response = await api.post<Recall>(`${BASE}/${companyId}/recalls/${recallId}/schedule`, {
      appointmentId,
    });
    return response.data;
  },

  async suggestions(companyId: string, since: number): Promise<RecallSuggestion[]> {
    const response = await api.get<RecallSuggestion[]>(`${BASE}/${companyId}/recalls/suggestions`, {
      params: { since },
    });
    return response.data;
  },

  async dismissSuggestion(companyId: string, contactId: string): Promise<void> {
    await api.post(`${BASE}/${companyId}/recalls/suggestions/${contactId}/dismiss`);
  },
};
