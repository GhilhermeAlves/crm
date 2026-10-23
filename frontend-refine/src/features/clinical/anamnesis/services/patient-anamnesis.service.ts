import api from "@/lib/api";
import type {
  PatientAnamnesisResponse,
  SavePatientAnamnesisRequest,
  StartPatientAnamnesisRequest,
} from "../types/patient-anamnesis.types";

export const patientAnamnesisService = {
  list: async (companyId: string, contactId: string): Promise<PatientAnamnesisResponse[]> => {
    const response = await api.get(`/companies/${companyId}/patients/${contactId}/anamneses`);
    return response.data;
  },
  getCurrent: async (
    companyId: string,
    contactId: string,
  ): Promise<PatientAnamnesisResponse | null> => {
    const response = await api.get(
      `/companies/${companyId}/patients/${contactId}/anamneses/current`,
      { validateStatus: (status: number) => status >= 200 && status < 300 },
    );
    if (response.status === 204) {
      return null;
    }
    return response.data;
  },
  get: async (
    companyId: string,
    contactId: string,
    anamnesisId: string,
  ): Promise<PatientAnamnesisResponse> => {
    const response = await api.get(
      `/companies/${companyId}/patients/${contactId}/anamneses/${anamnesisId}`,
    );
    return response.data;
  },
  start: async (
    companyId: string,
    contactId: string,
    payload: StartPatientAnamnesisRequest,
  ): Promise<PatientAnamnesisResponse> => {
    const response = await api.post(
      `/companies/${companyId}/patients/${contactId}/anamneses/start`,
      payload,
    );
    return response.data;
  },
  save: async (
    companyId: string,
    contactId: string,
    anamnesisId: string,
    payload: SavePatientAnamnesisRequest,
  ): Promise<PatientAnamnesisResponse> => {
    const response = await api.put(
      `/companies/${companyId}/patients/${contactId}/anamneses/${anamnesisId}`,
      payload,
    );
    return response.data;
  },
  discard: async (companyId: string, contactId: string, anamnesisId: string): Promise<void> => {
    await api.delete(`/companies/${companyId}/patients/${contactId}/anamneses/${anamnesisId}`);
  },
};
