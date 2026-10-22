import { api } from "@/lib/api";
import type {
  PatientEvolutionRequest,
  PatientEvolutionResponse,
} from "../types/evolution.types";

export const evolutionService = {
  list: async (companyId: string, contactId: string): Promise<PatientEvolutionResponse[]> => {
    const response = await api.get(
      `/companies/${companyId}/patients/${contactId}/evolutions`,
    );
    return response.data;
  },
  create: async (
    companyId: string,
    contactId: string,
    payload: PatientEvolutionRequest,
  ): Promise<PatientEvolutionResponse> => {
    const response = await api.post(
      `/companies/${companyId}/patients/${contactId}/evolutions`,
      payload,
    );
    return response.data;
  },
  update: async (
    companyId: string,
    contactId: string,
    evolutionId: string,
    payload: PatientEvolutionRequest,
  ): Promise<PatientEvolutionResponse> => {
    const response = await api.put(
      `/companies/${companyId}/patients/${contactId}/evolutions/${evolutionId}`,
      payload,
    );
    return response.data;
  },
  finalize: async (
    companyId: string,
    contactId: string,
    evolutionId: string,
  ): Promise<PatientEvolutionResponse> => {
    const response = await api.post(
      `/companies/${companyId}/patients/${contactId}/evolutions/${evolutionId}/finalize`,
    );
    return response.data;
  },
  delete: async (
    companyId: string,
    contactId: string,
    evolutionId: string,
  ): Promise<void> => {
    await api.delete(
      `/companies/${companyId}/patients/${contactId}/evolutions/${evolutionId}`,
    );
  },
};
