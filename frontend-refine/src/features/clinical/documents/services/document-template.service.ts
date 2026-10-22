import { api } from "@/lib/api";
import type {
  DocumentTemplateResponse,
} from "../types/document.types";

export const documentTemplateService = {
  list: async (companyId: string, activeOnly = true): Promise<DocumentTemplateResponse[]> => {
    const response = await api.get(`/companies/${companyId}/document-templates`, {
      params: activeOnly ? { activeOnly: true } : undefined,
    });
    return response.data;
  },
};
