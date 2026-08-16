import api from "@/lib/api";
import type {
  Opportunity,
  OpportunityHistory,
  Pipeline,
  PipelineMetrics,
  CreatePipelineRequest,
  CreateOpportunityRequest,
  UpdateOpportunityRequest,
  MoveDirection,
} from "../types/pipeline.types";

const BASE = "/companies";

export const PipelineService = {
  async list(companyId: string): Promise<Pipeline[]> {
    const response = await api.get<Pipeline[]>(
      `${BASE}/${companyId}/pipelines`,
    );
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Pipeline> {
    const response = await api.get<Pipeline>(
      `${BASE}/${companyId}/pipelines/${id}`,
    );
    return response.data;
  },

  async create(
    companyId: string,
    data: CreatePipelineRequest,
  ): Promise<Pipeline> {
    const response = await api.post<Pipeline>(
      `${BASE}/${companyId}/pipelines`,
      data,
    );
    return response.data;
  },

  async metrics(
    companyId: string,
    pipelineId: string,
  ): Promise<PipelineMetrics> {
    const response = await api.get<PipelineMetrics>(
      `${BASE}/${companyId}/pipelines/${pipelineId}/metrics`,
    );
    return response.data;
  },
};

export const OpportunityService = {
  async listByPipeline(
    companyId: string,
    pipelineId: string,
  ): Promise<Opportunity[]> {
    const response = await api.get<Opportunity[]>(
      `${BASE}/${companyId}/pipelines/${pipelineId}/opportunities`,
    );
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Opportunity> {
    const response = await api.get<Opportunity>(
      `${BASE}/${companyId}/opportunities/${id}`,
    );
    return response.data;
  },

  async create(
    companyId: string,
    pipelineId: string,
    data: CreateOpportunityRequest,
  ): Promise<Opportunity> {
    const response = await api.post<Opportunity>(
      `${BASE}/${companyId}/pipelines/${pipelineId}/opportunities`,
      data,
    );
    return response.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateOpportunityRequest,
  ): Promise<Opportunity> {
    const response = await api.put<Opportunity>(
      `${BASE}/${companyId}/opportunities/${id}`,
      data,
    );
    return response.data;
  },

  async move(
    companyId: string,
    id: string,
    direction: MoveDirection,
  ): Promise<Opportunity> {
    const response = await api.post<Opportunity>(
      `${BASE}/${companyId}/opportunities/${id}/move`,
      { direction },
    );
    return response.data;
  },

  async markWon(companyId: string, id: string): Promise<Opportunity> {
    const response = await api.post<Opportunity>(
      `${BASE}/${companyId}/opportunities/${id}/won`,
    );
    return response.data;
  },

  async markLost(
    companyId: string,
    id: string,
    lossReason: string,
  ): Promise<Opportunity> {
    const response = await api.post<Opportunity>(
      `${BASE}/${companyId}/opportunities/${id}/lost`,
      { lossReason },
    );
    return response.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/opportunities/${id}`);
  },

  async history(companyId: string, id: string): Promise<OpportunityHistory[]> {
    const response = await api.get<OpportunityHistory[]>(
      `${BASE}/${companyId}/opportunities/${id}/history`,
    );
    return response.data;
  },
};
