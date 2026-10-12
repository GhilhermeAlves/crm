import api from "@/lib/api";
import type {
  InventoryPage,
  InventoryFilters,
  RegisterMovementRequest,
  StockMovement,
  StockMovementPage,
} from "../types/inventory.types";

const base = (companyId: string) => `/companies/${companyId}/inventory`;

export const InventoryService = {
  async list(companyId: string, filters: InventoryFilters = {}): Promise<InventoryPage> {
    const response = await api.get<InventoryPage>(base(companyId), {
      params: {
        q: filters.q || undefined,
        category: filters.category || undefined,
        lowStock: filters.lowStock,
        active: filters.active,
        page: filters.page ?? 0,
        pageSize: filters.pageSize ?? 20,
      },
    });
    return response.data;
  },

  async listMovements(companyId: string, itemId: string, page = 0, pageSize = 20): Promise<StockMovementPage> {
    const response = await api.get<StockMovementPage>(`${base(companyId)}/${itemId}/movements`, {
      params: { page, pageSize },
    });
    return response.data;
  },

  async registerMovement(
    companyId: string,
    itemId: string,
    data: RegisterMovementRequest,
  ): Promise<StockMovement> {
    const response = await api.post<StockMovement>(`${base(companyId)}/${itemId}/movements`, data);
    return response.data;
  },
};