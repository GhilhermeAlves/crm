import api from "@/lib/api";
import type { StorageObject } from "../types/storage.types";

/**
 * Serviço de arquivos (Sprint 8.6). Escopado por tenant no backend
 * ({@code requireCompanyAccess} + RLS). O upload reutiliza o client HTTP único
 * do CRM e remove o {@code Content-Type} do request (via {@code null}) para o
 * browser montar o {@code multipart/form-data; boundary=...} do FormData — sem
 * fixar manualmente o valor, o que quebraria o boundary.
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
      // `null` remove o default `application/json` do client e faz o axios
      // passar o FormData intacto — o browser define o Content-Type multipart
      // com o boundary correto. (Não usar `undefined`: em axios 1.18.1 não
      // sobrescreve o default e o request acaba sem o boundary/part file.)
      { headers: { "Content-Type": null } },
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
