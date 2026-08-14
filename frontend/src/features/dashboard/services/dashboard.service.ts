import api from "@/lib/api";
import type { OperationalDashboard } from "../types/dashboard.types";

export const DashboardService = {
  async operational(companyId: string): Promise<OperationalDashboard> {
    const response = await api.get<OperationalDashboard>(
      `/companies/${companyId}/dashboard/operational`
    );
    return response.data;
  },
};