import { api } from "@/lib/api";
import type {
  IssuePatientDocumentRequest,
  PatientDocumentResponse,
  PreviewPatientDocumentRequest,
  PreviewPatientDocumentResponse,
} from "../types/document.types";

export const patientDocumentService = {
  list: async (companyId: string, contactId: string): Promise<PatientDocumentResponse[]> => {
    const response = await api.get(
      `/companies/${companyId}/patients/${contactId}/documents`,
    );
    return response.data;
  },
  preview: async (
    companyId: string,
    contactId: string,
    payload: PreviewPatientDocumentRequest,
  ): Promise<PreviewPatientDocumentResponse> => {
    const response = await api.post(
      `/companies/${companyId}/patients/${contactId}/documents/preview`,
      payload,
    );
    return response.data;
  },
  issue: async (
    companyId: string,
    contactId: string,
    payload: IssuePatientDocumentRequest,
  ): Promise<PatientDocumentResponse> => {
    const response = await api.post(
      `/companies/${companyId}/patients/${contactId}/documents`,
      payload,
    );
    return response.data;
  },
  openPdf: async (companyId: string, contactId: string, documentId: string): Promise<void> => {
    const response = await api.get(
      `/companies/${companyId}/patients/${contactId}/documents/${documentId}/pdf`,
      { responseType: "blob" },
    );
    const blob = new Blob([response.data], { type: "application/pdf" });
    const url = window.URL.createObjectURL(blob);
    window.open(url, "_blank", "noopener,noreferrer");
    setTimeout(() => window.URL.revokeObjectURL(url), 60000);
  },
  delete: async (
    companyId: string,
    contactId: string,
    documentId: string,
  ): Promise<void> => {
    await api.delete(
      `/companies/${companyId}/patients/${contactId}/documents/${documentId}`,
    );
  },
};
