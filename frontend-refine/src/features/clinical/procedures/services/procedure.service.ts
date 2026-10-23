import api from "@/lib/api";
import type {
  CompleteProcedureRequest,
  PatientProcedureRequest,
  PatientProcedureResponse,
  ProcedureFilters,
} from "../types/procedure.types";

const base = (companyId: string, contactId: string) =>
  `/companies/${companyId}/patients/${contactId}/procedures`;

export const procedureService = {
  list: async (
    companyId: string,
    contactId: string,
    filters: ProcedureFilters,
  ): Promise<PatientProcedureResponse[]> => {
    const response = await api.get(base(companyId, contactId), { params: filters });
    return response.data;
  },
  create: async (
    companyId: string,
    contactId: string,
    payload: PatientProcedureRequest,
  ): Promise<PatientProcedureResponse> => {
    const response = await api.post(base(companyId, contactId), payload);
    return response.data;
  },
  update: async (
    companyId: string,
    contactId: string,
    procedureId: string,
    payload: PatientProcedureRequest,
  ): Promise<PatientProcedureResponse> => {
    const response = await api.patch(`${base(companyId, contactId)}/${procedureId}`, payload);
    return response.data;
  },
  complete: async (
    companyId: string,
    contactId: string,
    procedureId: string,
    payload: CompleteProcedureRequest,
  ): Promise<PatientProcedureResponse> => {
    const response = await api.post(
      `${base(companyId, contactId)}/${procedureId}/complete`,
      payload,
    );
    return response.data;
  },
  delete: async (companyId: string, contactId: string, procedureId: string): Promise<void> => {
    await api.delete(`${base(companyId, contactId)}/${procedureId}`);
  },
};
