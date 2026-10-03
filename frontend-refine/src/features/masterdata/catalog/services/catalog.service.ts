import api from "@/lib/api";
import type {
  CatalogFilters,
  CatalogItem,
  CatalogItemRequest,
  CatalogPage,
} from "../types/catalog.types";

const base = (companyId: string) => `/companies/${companyId}/catalog`;

export const CatalogService = {
  async list(companyId: string, filters: CatalogFilters = {}): Promise<CatalogPage> {
    const response = await api.get<CatalogPage>(base(companyId), {
      params: {
        q: filters.q || undefined,
        type: filters.type,
        active: filters.active,
        page: filters.page ?? 0,
        pageSize: filters.pageSize ?? 20,
      },
    });
    return response.data;
  },

  async create(companyId: string, data: CatalogItemRequest): Promise<CatalogItem> {
    const response = await api.post<CatalogItem>(base(companyId), data);
    return response.data;
  },

  async update(companyId: string, id: string, data: CatalogItemRequest): Promise<CatalogItem> {
    const response = await api.put<CatalogItem>(`${base(companyId)}/${id}`, data);
    return response.data;
  },

  async setActive(companyId: string, id: string, active: boolean): Promise<CatalogItem> {
    const response = await api.post<CatalogItem>(
      `${base(companyId)}/${id}/${active ? "activate" : "deactivate"}`,
    );
    return response.data;
  },
};
