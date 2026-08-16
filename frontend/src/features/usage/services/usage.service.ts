import api from "@/lib/api";
import type { CompanyUsage } from "../types/usage.types";

/**
 * Uso/quota da empresa (Sprint 8.6). Escopado por tenant no backend; a
 * autorização retorna 403 quando o usuário não pertence à empresa.
 */
export const UsageService = {
  async companyUsage(companyId: string): Promise<CompanyUsage> {
    const response = await api.get<CompanyUsage>(
      `/companies/${companyId}/usage`,
    );
    return response.data;
  },
};
