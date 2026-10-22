import { api } from "@/lib/api";
import type {
  DocumentTemplateResponse,
  DocumentType,
} from "../types/document.types";

export type DocumentTemplateRequest = {
  type: DocumentType;
  title: string;
  body: string;
  active: boolean;
};

export const documentTemplateService = {
  list: async (companyId: string, activeOnly = false): Promise<DocumentTemplateResponse[]> => {
    const response = await api.get(`/companies/${companyId}/document-templates`, {
      params: { activeOnly },
    });
    return response.data;
  },
  create: async (companyId: string, payload: DocumentTemplateRequest): Promise<DocumentTemplateResponse> => {
    const response = await api.post(`/companies/${companyId}/document-templates`, payload);
    return response.data;
  },
  update: async (
    companyId: string,
    templateId: string,
    payload: DocumentTemplateRequest,
  ): Promise<DocumentTemplateResponse> => {
    const response = await api.put(`/companies/${companyId}/document-templates/${templateId}`, payload);
    return response.data;
  },
  delete: async (companyId: string, templateId: string): Promise<void> => {
    await api.delete(`/companies/${companyId}/document-templates/${templateId}`);
  },
};
