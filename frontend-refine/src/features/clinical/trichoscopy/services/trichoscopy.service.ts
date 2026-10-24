import api from "@/lib/api";
import type { TrichoscopyImageResponse } from "../types/trichoscopy.types";

const base = (companyId: string, contactId: string) =>
  `/companies/${companyId}/patients/${contactId}/trichoscopy`;

export const TrichoscopyService = {
  list: async (companyId: string, contactId: string): Promise<TrichoscopyImageResponse[]> => {
    const response = await api.get<TrichoscopyImageResponse[]>(base(companyId, contactId));
    return response.data;
  },

  capture: async (
    companyId: string,
    contactId: string,
    file: Blob,
    notes?: string,
  ): Promise<TrichoscopyImageResponse> => {
    const formData = new FormData();
    formData.append("file", file, "tricoscopia.jpg");
    if (notes) formData.append("notes", notes);
    const response = await api.post<TrichoscopyImageResponse>(
      base(companyId, contactId),
      formData,
      {
        headers: { "Content-Type": "multipart/form-data" },
      },
    );
    return response.data;
  },

  remove: async (companyId: string, contactId: string, imageId: string): Promise<void> => {
    await api.delete(`${base(companyId, contactId)}/${imageId}`);
  },
};
