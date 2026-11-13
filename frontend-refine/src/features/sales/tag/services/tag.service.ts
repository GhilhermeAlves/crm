import api from "@/lib/api";
import type { CreateTagRequest, Tag, UpdateTagRequest } from "../types/tag.types";

export class TagService {
  static async list(companyId: string): Promise<Tag[]> {
    const response = await api.get<Tag[]>(`/companies/${companyId}/tags`);
    return response.data;
  }

  static async create(companyId: string, data: CreateTagRequest): Promise<Tag> {
    const response = await api.post<Tag>(`/companies/${companyId}/tags`, data);
    return response.data;
  }

  static async update(companyId: string, id: string, data: UpdateTagRequest): Promise<Tag> {
    const response = await api.put<Tag>(`/companies/${companyId}/tags/${id}`, data);
    return response.data;
  }

  static async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`/companies/${companyId}/tags/${id}`);
  }
}
