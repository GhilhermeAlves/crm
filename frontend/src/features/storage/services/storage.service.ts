import api from "@/lib/api";
import type { StorageObject } from "../types/storage.types";

/**
 * Serviço de arquivos (Sprint 8.6). Escopado por tenant no backend
 * ({@code requireCompanyAccess} + RLS). O upload reutiliza o client HTTP único
 * do CRM e NÃO fixa {@code Content-Type} manualmente para não quebrar o
 * boundary do multipart.
 */
export const StorageService = {
  async list(companyId: string): Promise<StorageObject[]> {
    const response = await api.get<StorageObject[]>(`/companies/${companyId}/storage`);
    return response.data;
  },

  async upload(companyId: string, file: File): Promise<StorageObject> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.post<StorageObject>(
      `/companies/${companyId}/storage/upload`,
      formData,
      { headers: { "Content-Type": undefined } },
    );
    return response.data;
  },

  async download(companyId: string, objectId: string): Promise<Blob> {
    const response = await api.get<Blob>(`/companies/${companyId}/storage/${objectId}`, {
      responseType: "blob",
    });
    return response.data;
  },

  async remove(companyId: string, objectId: string): Promise<void> {
    await api.delete(`/companies/${companyId}/storage/${objectId}`);
  },
};
