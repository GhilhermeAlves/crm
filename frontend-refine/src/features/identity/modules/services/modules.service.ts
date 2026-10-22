import api from "@/lib/api";
import type { CompanyModules, UpdateCompanyModulesRequest } from "../types/modules.types";

const BASE = "/companies";

export const CompanyModulesService = {
  async get(companyId: string): Promise<CompanyModules> {
    const response = await api.get<CompanyModules>(`${BASE}/${companyId}/modules`);
    return response.data;
  },

  async update(companyId: string, data: UpdateCompanyModulesRequest): Promise<CompanyModules> {
    const response = await api.put<CompanyModules>(`${BASE}/${companyId}/modules`, data);
    return response.data;
  },
};
